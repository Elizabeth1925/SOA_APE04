# Modulo para funciones de hash criptograficas (SHA-256)
import hashlib

# Modulo para serializacion y deserializacion en formato JSON canonico
import json

# Generador de UUID version 4 para identificadores unicos de transacciones distribuidas
from uuid import uuid4

# Modulo de numeros decimales de precision arbitraria para importes monetarios
from decimal import Decimal

# Excepcion de base de datos cuando ocurre una violacion de clave unica
from sqlalchemy.exc import IntegrityError

# Fabrica de sesiones de base de datos y modelos ORM
from app.db import SessionLocal
from app.models import Venta, Comprador

# Repositorio de persistencia de ventas y cliente de comunicacion con el microservicio de Java
from app.repositories.venta_repository import VentaRepository
from app.clients.vehiculos_client import VehiculosClient, FalloRemoto
from app.clients.rabbitmq_publisher import publicar_evento_venta

# Excepcion de errores controlados de API
from app.errors import ApiError
# Funcion de autorizacion para validar si el usuario es propietario del recurso
from app.security import propietario

# Instancia singleton del cliente de vehiculos para llamar al microservicio en Java
remote = VehiculosClient()

# Formatea el objeto ORM Venta en un diccionario de respuesta JSON
def salida(v):
    precio_val = float(v.precio_acordado) if v.precio_acordado is not None else 0.0
    fecha_val = v.fecha.isoformat() + "Z" if v.fecha else None
    return {
        "id": v.id,
        "operacionId": v.operacion_id,
        "vehiculoId": v.vehiculo_id,
        "compradorId": v.comprador_id,
        "precio": precio_val,
        "precioAcordado": str(v.precio_acordado) if v.precio_acordado is not None else None,
        "moneda": v.moneda,
        "estado": v.estado,
        "notificacion": v.notificacion,
        "fecha": fecha_val,
        "fechaUTC": fecha_val,
    }

# Logica de negocio principal para la creacion idempotente y coordinada de una venta
def crear(datos, clave, usuario):
    # Genera un string JSON canonico con claves ordenadas y sin espacios innecesarios
    cuerpo = json.dumps(datos.model_dump(), sort_keys=True, separators=(",", ":"))
    # Calcula la huella digital SHA-256 del cuerpo de la peticion
    huella = hashlib.sha256(cuerpo.encode()).hexdigest()

    # Bandera para determinar si es un nuevo registro creado o una respuesta idempotente
    nueva = False

    # Abre la sesion de base de datos
    with SessionLocal() as db:
        repo = VentaRepository(db)

        # Busca si ya existia una venta con esta misma clave de idempotencia y usuario
        v = repo.por_clave(usuario["usuario"], clave)

        # Si no existe, procedemos a crear un nuevo registro de venta
        if v is None:
            # Obtiene el comprador asociado desde la base de datos
            comprador = db.get(Comprador, datos.compradorId)
            if comprador is None:
                raise ApiError(404, "COMPRADOR_NO_EXISTE", "Comprador inexistente")

            # Verifica que el comprador pertenezca al usuario autenticado (o sea admin)
            propietario(usuario, comprador.usuario)

            # Instancia la nueva venta con un operacion_id unico, datos de venta y clave idempotente
            v = Venta(
                operacion_id=str(uuid4()),
                usuario=usuario["usuario"],
                vehiculo_id=datos.vehiculoId,
                comprador_id=datos.compradorId,
                idempotency_key=clave,
                request_hash=huella,
            )

            # Agrega la entidad a la sesion
            db.add(v)
            try:
                # Confirma la insercion en base de datos
                db.commit()
                nueva = True
            except IntegrityError:
                # Manejo de concurrencia: si otra peticion simultanea ya inserto la misma clave
                db.rollback()
                v = repo.por_clave(usuario["usuario"], clave)
                if v is None:
                    raise

        # Si la venta ya existia pero con un cuerpo JSON diferente, rechaza con conflicto HTTP 409
        if v.request_hash != huella:
            raise ApiError(409, "CLAVE_REUTILIZADA", "La misma clave requiere el mismo cuerpo")

        # Guarda el ID generado de la venta para los siguientes pasos
        id = v.id

    # Intenta avanzar el estado de la venta interactuando con el microservicio Java con aislamiento transaccional
    procesar(id)

    # Si la venta concluyo exitosamente, publica evento en RabbitMQ de forma desacoplada
    notificar(id)

    # Consulta el estado final consolidado de la venta en base de datos
    with SessionLocal() as db:
        v = db.get(Venta, id)

        # Si la venta termino en fallo definitivo (por ejemplo vehiculo no disponible)
        if v.estado == "FALLIDA":
            raise ApiError(v.error_status or 409, "VENTA_RECHAZADA", v.error_detalle or "La venta fue rechazada")

        # Codigos HTTP: 201 si es nueva y completada, 200 si fue idempotente repetida, 202 si quedo pendiente/en revision
        status = (201 if nueva else 200) if v.estado == "COMPLETADA" else 202
        return salida(v), status

# Procesa la transaccion de venta comunicandose con el servicio Java de vehiculos
# CORREGIDO (Aislamiento Transaccional): Las llamadas HTTP remotas se ejecutan FUERA
# de las transacciones de MySQL, evitando retener conexiones o bloqueos durante llamadas de red.
def procesar(id):
    # Paso 1: Lectura inicial para verificar estado sin retener bloqueo
    with SessionLocal() as db:
        v = db.get(Venta, id)
        if v is None or v.estado in ("COMPLETADA", "FALLIDA"):
            return
        operacion_id = v.operacion_id
        vehiculo_id = v.vehiculo_id
        reserva_id_existente = v.reserva_id

    # Paso 2: Invocacion externa a la API Java (COMPLETAMENTE AISLADA DE LA BASE DE DATOS)
    nuevo_estado = "EN_REVISION"
    reserva_id = reserva_id_existente
    precio_acordado = None
    moneda = None
    error_status = None
    error_detalle = "Pendiente de consultar y conciliar con Java"

    try:
        r = None
        try:
            # Consulta si la operacion ya tiene una reserva en Java
            r = remote.por_operacion(operacion_id)
        except FalloRemoto as ex:
            if ex.status != 404:
                raise

            try:
                # Envia solicitud de reserva del vehiculo
                r = remote.reservar(vehiculo_id, operacion_id)
            except FalloRemoto as rechazo:
                if rechazo.status in (404, 409, 422):
                    nuevo_estado = "FALLIDA"
                    error_status = rechazo.status
                    error_detalle = "Vehículo inexistente o no disponible"
                else:
                    raise

        if r is not None:
            reserva_id = r["id"]
            precio_acordado = Decimal(str(r["precio"]))
            moneda = r["moneda"]

            if r["estado"] == "LIBERADA":
                nuevo_estado = "FALLIDA"
                error_status = 409
                error_detalle = "Reserva liberada"
            else:
                if r["estado"] == "RESERVADA":
                    r = remote.confirmar(r["id"])

                if r["estado"] != "CONFIRMADA":
                    raise FalloRemoto(502, "Estado de reserva inesperado")

                nuevo_estado = "COMPLETADA"
                error_status = None
                error_detalle = None

    except (FalloRemoto, ValueError, KeyError) as ex:
        nuevo_estado = "EN_REVISION"
        error_detalle = f"Fallo de comunicacion con Java: {ex}"

    # Paso 3: Transaccion corta de persistencia para actualizar el estado final
    with SessionLocal.begin() as db:
        v = VentaRepository(db).bloquear(id)
        if v is not None and v.estado not in ("COMPLETADA", "FALLIDA"):
            v.estado = nuevo_estado
            if reserva_id:
                v.reserva_id = reserva_id
            if precio_acordado is not None:
                v.precio_acordado = precio_acordado
            if moneda:
                v.moneda = moneda
            v.error_status = error_status
            v.error_detalle = error_detalle

# Publica el evento de venta en RabbitMQ una vez confirmada la operacion
# Aislamiento transaccional: la publicacion en RabbitMQ ocurre fuera de la transaccion de MySQL.
def notificar(id):
    # Paso 1: Lectura de datos de venta
    with SessionLocal() as db:
        v = db.get(Venta, id)
        # Solo publica si la venta esta COMPLETADA y el aviso aun no fue emitido
        if v is None or v.estado != "COMPLETADA" or v.notificacion == "ENVIADA":
            return
        venta_id = v.id
        vehiculo_id = v.vehiculo_id
        comprador_id = v.comprador_id
        operacion_id = v.operacion_id

    # Paso 2: Publicacion en RabbitMQ de forma independiente
    publicado_rmq = publicar_evento_venta(
        venta_id=venta_id,
        vehiculo_id=vehiculo_id,
        comprador_id=comprador_id,
        operacion_id=operacion_id,
    )

    # Paso 3: Actualizacion en MySQL del estado de notificacion
    with SessionLocal.begin() as db:
        v = VentaRepository(db).bloquear(id)
        if v is not None:
            v.notificacion = "ENVIADA" if publicado_rmq else "PENDIENTE"
