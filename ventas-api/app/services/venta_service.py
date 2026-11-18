# Modulo para funciones de hash criptograficas (SHA-256)
import hashlib

# Modulo para serializacion y deserializacion en formato JSON canonico
import json

# Generador de UUID version 4 para identificadores unicos de transacciones distribuidas
from uuid import uuid4

# Modulo de numeros decimales de precision arbitraria para importes monetarios
from decimal import Decimal

# Cliente HTTP para el envio de notificaciones
import httpx

# Excepcion de base de datos cuando ocurre una violacion de clave unica
from sqlalchemy.exc import IntegrityError

# Fabrica de sesiones de base de datos y modelos ORM
from app.db import SessionLocal
from app.models import Venta, Comprador

# Repositorio de persistencia de ventas y cliente de comunicacion con el microservicio de Java
from app.repositories.venta_repository import VentaRepository
from app.clients.vehiculos_client import VehiculosClient, FalloRemoto

# Configuraciones de URLs y claves de seguridad del servicio de notificaciones
from app.config import NOTIF_URL, NOTIF_KEY
# Excepcion de errores controlados de API
from app.errors import ApiError
# Funcion de autorizacion para validar si el usuario es propietario del recurso
from app.security import propietario

# Instancia singleton del cliente de vehiculos para llamar al microservicio en Java
remote = VehiculosClient()

# Formatea el objeto ORM Venta en un diccionario de respuesta JSON
def salida(v):
    return {"id": v.id, "operacionId": v.operacion_id,
            "vehiculoId": v.vehiculo_id, "compradorId": v.comprador_id,
            "precioAcordado": str(v.precio_acordado)
                if v.precio_acordado is not None else None,
            "moneda": v.moneda, "estado": v.estado,
            "notificacion": v.notificacion,
            "fechaUTC": v.fecha.isoformat() + "Z"}

# Logica de negocio principal para la creacion idempotente y coordinada de una venta
def crear(datos, clave, usuario):
    # Genera un string JSON canonico con claves ordenadas y sin espacios innecesarios
    cuerpo = json.dumps(datos.model_dump(), sort_keys=True,
                        separators=(",", ":"))
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
            v = Venta(operacion_id=str(uuid4()), usuario=usuario["usuario"],
                      vehiculo_id=datos.vehiculoId, comprador_id=datos.compradorId,
                      idempotency_key=clave, request_hash=huella)

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
            raise ApiError(409, "CLAVE_REUTILIZADA",
                           "La misma clave requiere el mismo cuerpo")

        # Guarda el ID generado de la venta para los siguientes pasos
        id = v.id

    # Intenta avanzar el estado de la venta interactuando con el microservicio Java
    procesar(id)

    # Si la venta concluyo exitosamente, envia la notificacion al comprador
    notificar(id)

    # Consulta el estado final consolidado de la venta en base de datos
    with SessionLocal() as db:
        v = db.get(Venta, id)

        # Si la venta termino en fallo definitivo (por ejemplo vehiculo no disponible)
        if v.estado == "FALLIDA":
            raise ApiError(v.error_status or 409, "VENTA_RECHAZADA",
                           v.error_detalle or "La venta fue rechazada")

        # Codigos HTTP: 201 si es nueva y completada, 200 si fue idempotente repetida, 202 si quedo pendiente/en revision
        status = (201 if nueva else 200) if v.estado == "COMPLETADA" else 202
        return salida(v), status

# Procesa la transaccion de venta comunicandose con el servicio Java de vehiculos
def procesar(id):
    # Inicia una transaccion con bloqueo de fila para evitar condiciones de carrera concurrentes
    with SessionLocal.begin() as db:
        v = VentaRepository(db).bloquear(id)

        # Si el registro no existe o ya culmino (completada o fallida), no hace nada
        if v is None or v.estado in ("COMPLETADA", "FALLIDA"):
            return

        try:
            try:
                # Consulta si la operacion ya tiene una reserva en Java
                r = remote.por_operacion(v.operacion_id)
            except FalloRemoto as ex:
                # Si no encontro reserva previa (404), intenta crearla
                if ex.status != 404:
                    raise

                try:
                    # Envia solicitud de reserva del vehiculo
                    r = remote.reservar(v.vehiculo_id, v.operacion_id)
                except FalloRemoto as rechazo:
                    # Si el vehiculo no existe, ya fue vendido o los datos son invalidos
                    if rechazo.status in (404, 409, 422):
                        v.estado = "FALLIDA"
                        v.error_status = rechazo.status
                        v.error_detalle = "Vehículo inexistente o no disponible"
                        return
                    raise

            # Almacena los datos de la reserva retornados por Java
            v.reserva_id = r["id"]
            v.precio_acordado = Decimal(str(r["precio"]))
            v.moneda = r["moneda"]

            # Si la reserva habia sido liberada previamente, marca la venta como fallida
            if r["estado"] == "LIBERADA":
                v.estado = "FALLIDA"
                v.error_status = 409
                v.error_detalle = "Reserva liberada"
                return

            # Si la reserva esta activa en estado RESERVADA, envia la confirmacion definitiva
            if r["estado"] == "RESERVADA":
                r = remote.confirmar(r["id"])

            # Si despues de confirmar el estado no es CONFIRMADA, lanza error remoto
            if r["estado"] != "CONFIRMADA":
                raise FalloRemoto(502, "Estado de reserva inesperado")

            # Marca la venta como COMPLETADA exitosamente
            v.estado = "COMPLETADA"
            v.error_status = None
            v.error_detalle = None

        except (FalloRemoto, ValueError, KeyError) as ex:
            # Si ocurre cualquier falla de red, timeout o respuesta incompleta, queda en revision para el worker
            v.estado = "EN_REVISION"
            v.error_detalle = "Pendiente de consultar y conciliar con Java"

# Envia la notificacion de venta completada al microservicio de notificaciones
def notificar(id):
    # Inicia una transaccion bloqueando el registro de la venta
    with SessionLocal.begin() as db:
        v = VentaRepository(db).bloquear(id)

        # Solo notifica si la venta esta COMPLETADA y la notificacion aun no fue enviada
        if v is None or v.estado != "COMPLETADA" or v.notificacion == "ENVIADA":
            return

        try:
            # Envia peticion POST al microservicio de notificaciones con timeout de 3 segundos
            r = httpx.post(NOTIF_URL + "/api/notificaciones", timeout=3,
                headers={"X-Notification-Key": NOTIF_KEY}, json={
                    "vehiculoId": v.vehiculo_id, "tipo": "VENTA",
                    "destinatario": v.comprador.correo,
                    "mensaje": f"Venta {v.id} operación {v.operacion_id} completada",
                })
            # Actualiza el estado segun si la respuesta HTTP fue exitosa (2xx)
            v.notificacion = "ENVIADA" if 200 <= r.status_code < 300 else "PENDIENTE"
        except httpx.RequestError:
            # Si falla la llamada HTTP a notificaciones, queda como PENDIENTE para reintento por el worker
            v.notificacion = "PENDIENTE"
