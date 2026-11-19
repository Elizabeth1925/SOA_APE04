import json
import logging
from datetime import datetime, timezone
from uuid import uuid4
import pika

from app.config import (
    RABBITMQ_HOST,
    RABBITMQ_PORT,
    RABBITMQ_USER,
    RABBITMQ_PASSWORD,
    RABBITMQ_QUEUE,
)

logger = logging.getLogger("rabbitmq_publisher")

def publicar_evento_venta(venta_id: int, vehiculo_id: int, comprador_id: int, operacion_id: str) -> bool:
    """
    Publica un evento persistente de venta completada en RabbitMQ.
    Se ejecuta de forma aislada e independiente fuera de la transaccion de base de datos.
    Contrato de mensaje:
      - eventoId: UUID v4 unico para deduplicacion en el consumidor
      - tipo: 'VENTA'
      - ventaId: ID numerico de la venta en MySQL
      - vehiculoId: ID del vehiculo vendido
      - compradorId: ID del comprador
      - operacionId: UUID de la transaccion distribuida
      - fechaUTC: ISO-8601 con zona horaria UTC
    """
    mensaje = {
        "eventoId": str(uuid4()),
        "tipo": "VENTA",
        "ventaId": venta_id,
        "vehiculoId": vehiculo_id,
        "compradorId": comprador_id,
        "operacionId": operacion_id,
        "fechaUTC": datetime.now(timezone.utc).isoformat(),
    }

    cuerpo = json.dumps(mensaje)

    try:
        credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASSWORD)
        parameters = pika.ConnectionParameters(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            credentials=credentials,
            connection_attempts=3,
            retry_delay=1,
            socket_timeout=3,
        )

        connection = pika.BlockingConnection(parameters)
        channel = connection.channel()

        # Asegurar cola durable
        channel.queue_declare(queue=RABBITMQ_QUEUE, durable=True)

        # Publicar con mensaje persistente (delivery_mode=2)
        channel.basic_publish(
            exchange="",
            routing_key=RABBITMQ_QUEUE,
            body=cuerpo,
            properties=pika.BasicProperties(
                delivery_mode=pika.DeliveryMode.Persistent,
                content_type="application/json",
            ),
        )

        connection.close()
        logger.info(f"Evento de venta publicado exitosamente en RabbitMQ: {mensaje}")
        return True
    except Exception as ex:
        logger.error(f"Fallo al publicar evento de venta en RabbitMQ: {ex}")
        return False
