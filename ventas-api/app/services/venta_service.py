"""
===============================================================================
ARCHIVO: app/services/venta_service.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Nucleo de logica de negocio y orquestacion distribuida de `ventas-api`.
    Implementa el ciclo de vida completo de una venta coordinando MySQL,
    el microservicio `vehiculos-api` (Spring Boot) y RabbitMQ sin mantener
    transacciones ni conexiones de base de datos abiertas durante llamadas de
    red (aislamiento transaccional en 3 fases para eliminar *connection hogging*).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]:
        * Idempotencia fuerte mediante `Idempotency-Key` + huella SHA-256 del
          JSON canonico + restriccion unica `uq_venta_usuario_clave`: retorna
          `201 Created` en creacion nueva, `200 OK` en reintento identico y
          `409 Conflict` (`CLAVE_REUTILIZADA`) si se reutiliza la clave con
          otro payload (P11).
        * Integridad estricta de precios: ignora cualquier precio enviado por
          el cliente y persiste exclusivamente `r["precio"]` y `r["moneda"]`
          obtenidos desde la reserva en Spring Boot (P18).
        * Aislamiento transaccional en 3 fases tanto en `procesar()` (llamadas
          HTTP a Java fuera de MySQL) como en `notificar()` (publicacion en
          RabbitMQ fuera de MySQL), cerrando la Fase 3 con bloqueo pesimista
          `SELECT ... FOR UPDATE`.
    - [Actividad 8]: Guardia estricta en `notificar(id)` que publica en
      RabbitMQ UNICAMENTE si `v.estado == "COMPLETADA"` y
      `v.notificacion != "ENVIADA"`, garantizando cero eventos espurios en
      ventas `FALLIDA`, `EN_REVISION` o reintentos idempotentes, y dejando
      `notificacion = "PENDIENTE"` si RabbitMQ cae para recuperacion por
      `app/worker.py`.
    - [Actividad 10 - P27]: Coherencia transversal de `id` (`ventaId`),
      `operacionId` (UUID v4 unico), `vehiculoId` y `compradorId` en MySQL,
      en la respuesta REST (`salida(v)`), en las llamadas a Java y en el
      evento RabbitMQ.
    - [Actividad 11]: Manejo real de fallos sin confirmaciones ficticias:
      rechazos de vehiculo (`404`/`409`/`422`) -> estado `FALLIDA` y HTTP real
      (P12); caida/timeout de Java -> estado `EN_REVISION` y HTTP `202` (P13);
      caida de RabbitMQ -> `notificacion = "PENDIENTE"` recuperable (P13).
===============================================================================
"""

# [Actividad 4] Algoritmo criptografico SHA-256 usado para calcular la huella digital del cuerpo de la peticion
import hashlib

# [Actividad 4] Serializacion JSON determinista (`sort_keys=True`) para garantizar huellas SHA-256 reproducibles
import json

# [Actividad 10 - P27] Generador de UUID v4 para crear el `operacion_id` unico de cada transaccion distribuida
from uuid import uuid4

# [Actividad 4] Tipo `Decimal` de precision exacta para almacenar `precio_acordado` proveniente de Spring Boot
from decimal import Decimal

# [Actividad 4] Captura de colisiones simultaneas sobre la restriccion unica `uq_venta_usuario_clave`
from sqlalchemy.exc import IntegrityError

# [Actividad 4] Fabrica de sesiones SQLAlchemy y modelos ORM `Venta` y `Comprador`
from app.db import SessionLocal
from app.models import Venta, Comprador

# [Actividad 4] [Actividad 8] Repositorio de acceso a datos con consulta por clave y bloqueo `FOR UPDATE`
from app.repositories.venta_repository import VentaRepository
# [Actividad 4] [Actividad 11] Cliente HTTP hacia Spring Boot y su excepcion de fallo remoto
from app.clients.vehiculos_client import VehiculosClient, FalloRemoto
# [Actividad 8] Publicador desacoplado de eventos persistentes hacia la cola durable de RabbitMQ
from app.clients.rabbitmq_publisher import publicar_evento_venta

# [Actividad 4] [Actividad 11] Excepcion controlada que emite respuestas RFC 7807
from app.errors import ApiError
# [Actividad 4] [Actividad 11] Guardia RBAC para verificar propiedad del comprador usado en la venta
from app.security import propietario

# [Actividad 4] Instancia reutilizable del cliente HTTP hacia el microservicio de Vehiculos en Java
remote = VehiculosClient()


# [Actividad 4] [Actividad 10 - P27] Serializa un modelo ORM `Venta` al contrato JSON uniforme de la API REST
def salida(v):
    """Construye el diccionario de respuesta de una venta manteniendo coherencia de IDs, precio y fechas UTC."""
    # Convierte el `Decimal` acordado a `float` (o `0.0` si aun no se ha fijado reserva) para clientes frontend
    precio_val = float(v.precio_acordado) if v.precio_acordado is not None else 0.0
    # Formatea la fecha UTC naive de MySQL en estandar ISO-8601 con sufijo `Z`
    fecha_val = v.fecha.isoformat() + "Z" if v.fecha else None
    # Retorna la representacion JSON con alias compatibles para PWA React, Windows Forms y pruebas APE 04
    return {
        # [Actividad 10 - P27] Identificador primario de la venta (`ventaId`)
        "id": v.id,
        # [Actividad 10 - P27] UUID transversal de correlacion distribuida con Java y RabbitMQ
        "operacionId": v.operacion_id,
        # [Actividad 10 - P27] Identificador del vehiculo vendido en Java
        "vehiculoId": v.vehiculo_id,
        # [Actividad 10 - P27] Identificador del comprador en MySQL
        "compradorId": v.comprador_id,
        # [Actividad 4] Precio numerico oficial obtenido desde Spring Boot (nunca el manipulado por el cliente)
        "precio": precio_val,
        # [Actividad 4] Representacion textual exacta del `Decimal` oficial (`precioAcordado`) sin perdida de precision
        "precioAcordado": str(v.precio_acordado) if v.precio_acordado is not None else None,
        # [Actividad 4] Moneda oficial retornada por Spring Boot (ej. `USD`)
        "moneda": v.moneda,
        # [Actividad 4] [Actividad 11] Estado actual (`PENDIENTE`, `EN_REVISION`, `COMPLETADA` o `FALLIDA`)
        "estado": v.estado,
        # [Actividad 8] Estado Outbox de notificacion RabbitMQ (`PENDIENTE` o `ENVIADA`)
        "notificacion": v.notificacion,
        # Marca temporal ISO-8601 UTC de creacion de la venta
        "fecha": fecha_val,
        # Alias explicito `fechaUTC` usado por consumidores y auditoria transversal
        "fechaUTC": fecha_val,
    }


# [Actividad 4] [Actividad 8] [Actividad 10 - P27] [Actividad 11] Orquesta la creacion idempotente de una venta
def crear(datos, clave, usuario):
    """
    Garantiza idempotencia por `(usuario, Idempotency-Key)` + SHA-256, ejecuta
    el procesamiento remoto aislado de BD, dispara la notificacion RabbitMQ si
    aplica y retorna `(payload_venta, http_status)`.
    """
    # [Actividad 4] Serializa el DTO normalizado en JSON canonico (claves ordenadas, sin espacios) para hash estable
    cuerpo = json.dumps(datos.model_dump(), sort_keys=True, separators=(",", ":"))
    # [Actividad 4] Calcula la huella digital SHA-256 hexadecimal (64 caracteres) del cuerpo de la solicitud
    huella = hashlib.sha256(cuerpo.encode()).hexdigest()

    # [Actividad 4] Indica si la fila fue insertada en esta invocacion (`201`) o recuperada como reintento idempotente (`200`)
    nueva = False

    # [Actividad 4] Transaccion corta inicial: verifica idempotencia y, si es nueva, registra la intencion `PENDIENTE`
    with SessionLocal() as db:
        # Instancia el repositorio sobre la sesion local
        repo = VentaRepository(db)

        # [Actividad 4] Consulta si ya existe una venta registrada para `(usuario, Idempotency-Key)` (P11)
        v = repo.por_clave(usuario["usuario"], clave)

        # [Actividad 4] Camino de creacion nueva cuando no se encontro registro previo para la clave
        if v is None:
            # Verifica la existencia del comprador referenciado en MySQL antes de crear la venta
            comprador = db.get(Comprador, datos.compradorId)
            # Si el comprador no existe, aborta inmediatamente con HTTP 404 RFC 7807 sin tocar Java ni RabbitMQ
            if comprador is None:
                raise ApiError(404, "COMPRADOR_NO_EXISTE", "Comprador inexistente")

            # [Actividad 4] [Actividad 11] Valida que el comprador pertenezca al usuario autenticado o que este sea `ROLE_ADMIN` (P10 -> 403)
            propietario(usuario, comprador.usuario)

            # [Actividad 4] [Actividad 10 - P27] Construye la nueva entidad `Venta` en estado inicial `PENDIENTE` y `notificacion = PENDIENTE`
            v = Venta(
                # Genera UUID v4 unico para correlacionar la reserva en Spring Boot y el evento RabbitMQ
                operacion_id=str(uuid4()),
                # Registra el usuario propietario autenticado
                usuario=usuario["usuario"],
                # Enlaza el vehiculo solicitado (`vehiculoId`)
                vehiculo_id=datos.vehiculoId,
                # Enlaza el comprador validado (`compradorId`)
                comprador_id=datos.compradorId,
                # Almacena la cabecera `Idempotency-Key`
                idempotency_key=clave,
                # Almacena la huella SHA-256 del cuerpo canonico para futuras validaciones de idempotencia
                request_hash=huella,
            )

            # Agrega la nueva venta a la sesion SQLAlchemy
            db.add(v)
            # [Actividad 4] Persiste con proteccion ante carreras concurrentes mediante la restriccion unica `uq_venta_usuario_clave`
            try:
                # Confirma la fila `PENDIENTE` en MySQL y libera la transaccion antes de hacer llamadas HTTP a Java
                db.commit()
                # Marca que esta peticion fue la ganadora en crear el registro (permitira responder HTTP 201)
                nueva = True
            # [Actividad 4] [Actividad 11] Si otra peticion concurrente inserto exactamente la misma clave milisegundos antes
            except IntegrityError:
                # Revierte la insercion fallida para limpiar el estado de la sesion
                db.rollback()
                # Recupera la fila ganadora ya persistida para evaluarla de forma idempotente
                v = repo.por_clave(usuario["usuario"], clave)
                # Si el `IntegrityError` tuvo otra causa (no la clave de idempotencia), propaga la excepcion original
                if v is None:
                    raise

        # [Actividad 4] [Actividad 11] Si la misma `Idempotency-Key` se reutiliza con un payload diferente, rechaza con HTTP 409 (P11)
        if v.request_hash != huella:
            raise ApiError(409, "CLAVE_REUTILIZADA", "La misma clave requiere el mismo cuerpo")

        # Extrae el identificador numerico primario antes de cerrar la sesion inicial
        id = v.id

    # [Actividad 4] [Actividad 11] Ejecuta la coordinacion remota con Spring Boot en 3 fases aisladas (sin retener conexion MySQL)
    procesar(id)

    # [Actividad 8] Ejecuta la publicacion desacoplada en RabbitMQ con guardia estricta (solo si `COMPLETADA` y no `ENVIADA`)
    notificar(id)

    # [Actividad 4] [Actividad 11] Lectura final corta para devolver el estado real consolidado en MySQL
    with SessionLocal() as db:
        # Carga la venta actualizada tras `procesar()` y `notificar()`
        v = db.get(Venta, id)

        # [Actividad 11] Si Java rechazo definitivamente la operacion (P12 -> 404/409/422), responde error RFC 7807 sin simular exito
        if v.estado == "FALLIDA":
            raise ApiError(v.error_status or 409, "VENTA_RECHAZADA", v.error_detalle or "La venta fue rechazada")

        # [Actividad 4] [Actividad 11] Selecciona el codigo HTTP semantico exacto:
        # - `201 Created`: nueva venta `COMPLETADA`
        # - `200 OK`: reintento idempotente de venta ya `COMPLETADA` (P11)
        # - `202 Accepted`: venta en `PENDIENTE`/`EN_REVISION` por fallo temporal de Java, pendiente de conciliacion por `worker.py` (P13)
        status = (201 if nueva else 200) if v.estado == "COMPLETADA" else 202
        # Retorna la representacion JSON canonica junto con el codigo HTTP correspondiente
        return salida(v), status


# [Actividad 4] [Actividad 11] Aislamiento transaccional en 3 fases para coordinar reservas y confirmaciones en Spring Boot.
# Evita *connection hogging* al ejecutar TODA la comunicacion HTTP fuera de sesiones y bloqueos de MySQL.
def procesar(id):
    """
    Coordina en 3 fases aisladas el ciclo de reserva y confirmacion con `vehiculos-api` (Java):
      - Fase 1: Lectura rapida en MySQL (sin bloqueo) para extraer IDs y salir si ya es estado terminal.
      - Fase 2: Llamadas HTTP a Java (`por_operacion`, `reservar`, `confirmar`) sin conexion DB abierta.
      - Fase 3: Transaccion corta en MySQL con bloqueo pesimista `FOR UPDATE` para persistir estado y precio oficial.
    """
    # [Actividad 4] FASE 1 (Lectura corta sin bloqueo): obtiene datos necesarios y cierra inmediatamente la conexion a MySQL
    with SessionLocal() as db:
        # Recupera la venta por clave primaria
        v = db.get(Venta, id)
        # Si no existe o ya alcanzo un estado terminal (`COMPLETADA` o `FALLIDA`), no realiza ninguna llamada remota
        if v is None or v.estado in ("COMPLETADA", "FALLIDA"):
            return
        # Copia en variables locales los datos inmutables requeridos por la Fase 2 fuera de la sesion
        operacion_id = v.operacion_id
        # Identificador del vehiculo a reservar en Spring Boot
        vehiculo_id = v.vehiculo_id
        # Identificador de reserva previa si ya existia de un intento anterior
        reserva_id_existente = v.reserva_id

    # [Actividad 4] [Actividad 11] FASE 2 (Comunicacion HTTP con Java COMPLETAMENTE AISLADA de MySQL)
    # Inicializa el estado conservador `EN_REVISION` para nunca confirmar ficticiamente si ocurre un fallo inesperado
    nuevo_estado = "EN_REVISION"
    # Mantiene la referencia a la reserva previa si ya se conocia
    reserva_id = reserva_id_existente
    # Inicializa el precio acordado oficial que sera poblado estrictamente desde la respuesta de Java (P18)
    precio_acordado = None
    # Inicializa el codigo ISO de moneda oficial que sera poblado desde Java
    moneda = None
    # Inicializa el codigo HTTP de error remoto (solo se llena en fallos definitivos)
    error_status = None
    # Mensaje diagnostico por defecto mientras la conciliacion con Java siga abierta
    error_detalle = "Pendiente de consultar y conciliar con Java"

    # [Actividad 4] [Actividad 11] Ejecuta el protocolo idempotente contra Spring Boot: consultar -> reservar -> confirmar
    try:
        # Contenedor para la respuesta JSON de la reserva devuelta por Java
        r = None
        # Intenta primero recuperar una reserva existente asociada a `operacion_id` (seguro ante reintentos y timeouts previos)
        try:
            # Llama a `GET /api/reservas?operacionId=...` en Java
            r = remote.por_operacion(operacion_id)
        # Captura el resultado cuando Java no encuentra reserva previa o falla la consulta
        except FalloRemoto as ex:
            # Si el error no es `404` (por ejemplo `503` o `500`), relanza para dejar la venta en `EN_REVISION` (P13)
            if ex.status != 404:
                raise

            # Si Java respondio `404`, la reserva aun no existia: solicita crearla ahora
            try:
                # [Actividad 4] [Actividad 10 - P27] Llama a `POST /api/vehiculos/{vehiculo_id}/reservas` con `operacionId`
                r = remote.reservar(vehiculo_id, operacion_id)
            # [Actividad 11] Evalua si el rechazo al reservar es definitivo (vehiculo inexistente, vendido o invalido)
            except FalloRemoto as rechazo:
                # Los codigos `404`, `409` y `422` representan rechazos definitivos de negocio en Java (P12)
                if rechazo.status in (404, 409, 422):
                    # Transiciona a estado terminal `FALLIDA` (evitara publicar evento en RabbitMQ)
                    nuevo_estado = "FALLIDA"
                    # Conserva el codigo HTTP real recibido de Spring Boot
                    error_status = rechazo.status
                    # Registra el motivo legible del rechazo
                    error_detalle = "Vehículo inexistente o no disponible"
                else:
                    # Otros codigos (ej. `500`/`503`) son transitorios: relanza para quedar en `EN_REVISION` y reintentar en worker
                    raise

        # [Actividad 4] Si se obtuvo un objeto de reserva valido desde Java, extrae sus datos autoritativos y confirma
        if r is not None:
            # Captura el UUID oficial de la reserva generado por Spring Boot
            reserva_id = r["id"]
            # [Actividad 4] [Actividad 11] INTEGRIDAD ESTRICTA DE PRECIOS (P18): toma `Decimal(str(r["precio"]))` SOLO desde Java
            precio_acordado = Decimal(str(r["precio"]))
            # Toma la moneda oficial del catalogo desde Spring Boot
            moneda = r["moneda"]

            # [Actividad 11] Si la reserva fue liberada/expirada en Java, la venta no puede completarse
            if r["estado"] == "LIBERADA":
                # Marca la venta como `FALLIDA` definitiva con HTTP 409
                nuevo_estado = "FALLIDA"
                error_status = 409
                error_detalle = "Reserva liberada"
            else:
                # [Actividad 4] Si la reserva esta en estado `RESERVADA`, ejecuta el segundo paso: confirmacion definitiva
                if r["estado"] == "RESERVADA":
                    # Llama a `PUT /api/reservas/{id}/confirmacion` en Spring Boot
                    r = remote.confirmar(r["id"])

                # [Actividad 11] Verifica estrictamente que Spring Boot haya confirmado realmente la reserva
                if r["estado"] != "CONFIRMADA":
                    # Rechaza cualquier estado inesperado en lugar de fabricar una confirmacion falsa
                    raise FalloRemoto(502, "Estado de reserva inesperado")

                # [Actividad 4] Solo tras `estado == "CONFIRMADA"` real en Java se marca la venta como `COMPLETADA`
                nuevo_estado = "COMPLETADA"
                # Limpia cualquier error transitorio de intentos previos
                error_status = None
                error_detalle = None

    # [Actividad 11] Ante cualquier fallo remoto transitorio o formato invalido, preserva `EN_REVISION` sin confirmar falsamente (P13)
    except (FalloRemoto, ValueError, KeyError) as ex:
        # Mantiene estado recuperable para conciliacion automatica posterior por `app/worker.py`
        nuevo_estado = "EN_REVISION"
        # Guarda el diagnostico tecnico real del fallo de integracion
        error_detalle = f"Fallo de comunicacion con Java: {ex}"

    # [Actividad 4] FASE 3 (Transaccion corta con bloqueo pesimista `FOR UPDATE` en MySQL)
    with SessionLocal.begin() as db:
        # Adquiere bloqueo exclusivo sobre la fila `ventas.id` para aplicar la transicion de estado sin condiciones de carrera
        v = VentaRepository(db).bloquear(id)
        # Guardia de monotonicidad: solo actualiza si la fila existe y otro proceso no la llevo ya a un estado terminal
        if v is not None and v.estado not in ("COMPLETADA", "FALLIDA"):
            # Persiste el nuevo estado resultante (`COMPLETADA`, `FALLIDA` o `EN_REVISION`)
            v.estado = nuevo_estado
            # Si se obtuvo `reserva_id` desde Java, lo persiste para trazabilidad y futuras conciliaciones
            if reserva_id:
                v.reserva_id = reserva_id
            # [Actividad 4] [Actividad 11] Si Java devolvio precio oficial, lo guarda de manera autoritativa (P18)
            if precio_acordado is not None:
                v.precio_acordado = precio_acordado
            # Si Java devolvio moneda oficial, la persiste
            if moneda:
                v.moneda = moneda
            # Actualiza el codigo de error HTTP real (o `None` al completarse)
            v.error_status = error_status
            # Actualiza el mensaje de detalle de error/revision (o `None` al completarse)
            v.error_detalle = error_detalle


# [Actividad 8] [Actividad 10 - P27] [Actividad 11] Publica el evento durable de venta en RabbitMQ siguiendo el mismo
# aislamiento transaccional en 3 fases (nunca mantiene abierta una transaccion de MySQL durante I/O AMQP).
def notificar(id):
    """
    Aplica guardia estricta (`estado == COMPLETADA` y `notificacion != ENVIADA`),
    publica el evento en RabbitMQ fuera de BD y actualiza el estado Outbox en MySQL.
    """
    # [Actividad 8] FASE 1 (Lectura rapida y guardia estricta): verifica elegibilidad y cierra la sesion MySQL
    with SessionLocal() as db:
        # Consulta la venta actualizada en MySQL
        v = db.get(Venta, id)
        # [Actividad 8] GUARDIA ESTRICTA:
        #   - Nunca publica si la venta no existe
        #   - Nunca publica si la venta esta `FALLIDA`, `PENDIENTE` o `EN_REVISION` (P12 / P13)
        #   - Nunca republica si `notificacion == "ENVIADA"` (evita duplicados en reintentos idempotentes P11)
        if v is None or v.estado != "COMPLETADA" or v.notificacion == "ENVIADA":
            return
        # [Actividad 10 - P27] Extrae unicamente los 4 identificadores transversales sin copiar PII (correo/documento/nombre) ni JWT
        venta_id = v.id
        vehiculo_id = v.vehiculo_id
        comprador_id = v.comprador_id
        operacion_id = v.operacion_id

    # [Actividad 8] FASE 2 (Publicacion AMQP aislada de la BD): envia mensaje persistente a la cola `notificaciones.ventas`
    publicado_rmq = publicar_evento_venta(
        # [Actividad 10 - P27] ID de venta en MySQL
        venta_id=venta_id,
        # [Actividad 10 - P27] ID de vehiculo en Spring Boot
        vehiculo_id=vehiculo_id,
        # [Actividad 10 - P27] ID de comprador en MySQL
        comprador_id=comprador_id,
        # [Actividad 10 - P27] UUID de operacion distribuida
        operacion_id=operacion_id,
    )

    # [Actividad 8] [Actividad 11] FASE 3 (Transaccion corta con bloqueo `FOR UPDATE`): registra el resultado real del patron Outbox
    with SessionLocal.begin() as db:
        # Bloquea la fila de la venta para actualizar atomicamente su estado de notificacion
        v = VentaRepository(db).bloquear(id)
        # Si la venta existe, marca `ENVIADA` solo si RabbitMQ confirmo publicacion; si fallo (P13), deja `PENDIENTE` para `worker.py`
        if v is not None:
            v.notificacion = "ENVIADA" if publicado_rmq else "PENDIENTE"
