"""
===============================================================================
ARCHIVO: app/clients/rabbitmq_publisher.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Modulo desacoplado de mensajeria asincrona encargado de publicar eventos
    durables de venta completada hacia RabbitMQ (`notificaciones.ventas`) para
    que sean consumidos de forma asincrona e idempotente por `notificaciones-api`.
    Se invoca exclusivamente desde la Fase 2 de `venta_service.notificar()`,
    completamente fuera de cualquier transaccion activa de MySQL.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 8]: Publicacion durable en RabbitMQ con cola persistente
      (`durable=True`), mensajes persistentes (`delivery_mode=2` /
      `pika.DeliveryMode.Persistent`), `content_type="application/json"` y
      payload minimo sin PII (ni correo, ni documento, ni nombre) ni tokens JWT.
    - [Actividad 10 - P27]: Garantiza coherencia transversal del contrato de
      evento enviando `ventaId`, `vehiculoId`, `compradorId`, `operacionId`,
      `eventoId` (deduplicacion en consumidor) y `fechaUTC`.
    - [Actividad 11]: Captura fallos reales del broker RabbitMQ retornando
      `False` sin lanzar excepcion ni falsificar `notificacion = "ENVIADA"`,
      habilitando recuperacion real mediante `worker.py` (patron Outbox, P13).
===============================================================================
"""

# [Actividad 8] Serializador JSON estandar para construir el cuerpo del evento AMQP
import json
# [Actividad 8] [Actividad 11] Modulo de logging para auditar publicaciones exitosas y fallos reales de RabbitMQ
import logging
# [Actividad 8] [Actividad 10 - P27] Utilidades de fecha/hora para sellar cada evento con marca temporal `fechaUTC` en ISO-8601
from datetime import datetime, timezone
# [Actividad 8] Generador UUID v4 para asignar un `eventoId` unico que permita deduplicacion en el consumidor
from uuid import uuid4
# [Actividad 8] Cliente oficial AMQP 0-9-1 (`pika`) para conectar y publicar en RabbitMQ
import pika

# [Actividad 1 / Actividad 2] [Actividad 8] Importa parametros de conexion y nombre de cola RabbitMQ leidos desde `.env`
from app.config import (
    # Host del broker RabbitMQ
    RABBITMQ_HOST,
    # Puerto AMQP del broker RabbitMQ (5672)
    RABBITMQ_PORT,
    # Usuario de autenticacion AMQP
    RABBITMQ_USER,
    # Contrasena de autenticacion AMQP
    RABBITMQ_PASSWORD,
    # Nombre de la cola durable (`notificaciones.ventas`)
    RABBITMQ_QUEUE,
)

# [Actividad 8] Logger dedicado para trazar la actividad del publicador RabbitMQ
logger = logging.getLogger("rabbitmq_publisher")


# [Actividad 8] [Actividad 10 - P27] [Actividad 11] Publica un evento persistente de venta completada sin PII ni JWT
def publicar_evento_venta(venta_id: int, vehiculo_id: int, comprador_id: int, operacion_id: str) -> bool:
    """
    Publica un evento persistente de venta completada en RabbitMQ.
    Se ejecuta de forma aislada e independiente fuera de la transaccion de base de datos.
    Contrato de mensaje ([Actividad 8] y [Actividad 10 - P27], sin PII ni JWT):
      - eventoId: UUID v4 unico para deduplicacion en el consumidor
      - tipo: 'VENTA'
      - ventaId: ID numerico de la venta en MySQL
      - vehiculoId: ID del vehiculo vendido
      - compradorId: ID del comprador
      - operacionId: UUID de la transaccion distribuida
      - fechaUTC: ISO-8601 con zona horaria UTC
    """
    # [Actividad 8] [Actividad 10 - P27] Construye el diccionario del contrato de evento estrictamente libre de PII y credenciales
    mensaje = {
        # Identificador unico de este mensaje para control de idempotencia/deduplicacion en `notificaciones-api`
        "eventoId": str(uuid4()),
        # Tipo semantico de evento del dominio de negocio
        "tipo": "VENTA",
        # [Actividad 10 - P27] Identificador primario de la venta en MySQL
        "ventaId": venta_id,
        # [Actividad 10 - P27] Identificador del vehiculo confirmado en Spring Boot
        "vehiculoId": vehiculo_id,
        # [Actividad 10 - P27] Identificador del comprador asociado en MySQL
        "compradorId": comprador_id,
        # [Actividad 10 - P27] UUID transversal de correlacion entre `ventas-api`, `vehiculos-api` y `notificaciones-api`
        "operacionId": operacion_id,
        # Marca temporal exacta en formato ISO-8601 con zona horaria UTC
        "fechaUTC": datetime.now(timezone.utc).isoformat(),
    }

    # [Actividad 8] Serializa el diccionario del evento a una cadena JSON UTF-8
    cuerpo = json.dumps(mensaje)

    # [Actividad 8] [Actividad 11] Intenta conectar y publicar de forma acotada; cualquier fallo retorna `False` para reintento Outbox
    try:
        # Construye las credenciales AMQP planas usando los valores parametrizados en `.env`
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
        # Configura parametros de conexion con reintentos cortos y timeout de socket para no bloquear la API REST
        parameters = pika.ConnectionParameters(
            # Direccion IP o host del broker RabbitMQ
            host=RABBITMQ_HOST,
            # Puerto AMQP del broker
            port=RABBITMQ_PORT,
            # Credenciales configuradas en el entorno
            credentials=credentials,
            # Maximo de 3 intentos rapidos de apertura TCP antes de rendirse y delegar al worker Outbox
            connection_attempts=3,
            # Espera de 1 segundo entre intentos de conexion
            retry_delay=1,
            # Timeout de socket de 3 segundos para evitar colgar peticiones si RabbitMQ no responde
            socket_timeout=3,
        )

        # Abre la conexion bloqueante AMQP contra RabbitMQ
        connection = pika.BlockingConnection(parameters)
        # Crea un canal AMQP sobre la conexion establecida
        channel = connection.channel()

        # [Actividad 8] Declara de forma idempotente la cola `notificaciones.ventas` como durable (sobrevive reinicios del broker)
        channel.queue_declare(queue=RABBITMQ_QUEUE, durable=True)

        # [Actividad 8] Publica el mensaje en el exchange por defecto enrutado directamente a la cola durable
        channel.basic_publish(
            # Exchange vacio (`""`) indica enrutamiento directo por nombre exacto de cola (`routing_key`)
            exchange="",
            # Nombre de la cola destino (`notificaciones.ventas`)
            routing_key=RABBITMQ_QUEUE,
            # Carga util JSON serializada sin datos personales ni tokens
            body=cuerpo,
            # Metadatos AMQP que garantizan persistencia en disco y tipo de contenido JSON
            properties=pika.BasicProperties(
                # `delivery_mode=2` (`Persistent`) ordena a RabbitMQ persistir el mensaje en disco
                delivery_mode=pika.DeliveryMode.Persistent,
                # Declara el formato MIME del cuerpo para los consumidores AMQP
                content_type="application/json",
            ),
        )

        # Cierra ordenadamente la conexion AMQP liberando sockets y recursos del broker
        connection.close()
        # Registra en el log la publicacion exitosa del evento (sin PII ni secretos)
        logger.info(f"Evento de venta publicado exitosamente en RabbitMQ: {mensaje}")
        # Indica a `venta_service.notificar()` que puede marcar `notificacion = "ENVIADA"` en MySQL
        return True
    # [Actividad 11] Captura cualquier fallo de red, autenticacion o caida del broker RabbitMQ (P13)
    except Exception as ex:
        # Registra el error real en el log del servidor para diagnostico
        logger.error(f"Fallo al publicar evento de venta en RabbitMQ: {ex}")
        # Retorna `False` para conservar `notificacion = "PENDIENTE"` en MySQL y permitir recuperacion por `worker.py`
        return False
