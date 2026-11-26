"""
===============================================================================
ARCHIVO: app/clients/__init__.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Inicializador del subpaquete `app.clients`, que agrupa los adaptadores de
    salida desacoplados hacia componentes externos del ecosistema SOA:
    - `vehiculos_client.py`: cliente HTTP autenticado hacia Spring Boot (Java).
    - `rabbitmq_publisher.py`: publicador AMQP durable hacia RabbitMQ.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Encapsulamiento de llamadas HTTP hacia `vehiculos-api`.
    - [Actividad 8]: Encapsulamiento del publicador asincrono RabbitMQ.
===============================================================================
"""

# [Actividad 4] [Actividad 8] Marca la carpeta `clients` como subpaquete Python para integraciones externas HTTP y AMQP.
