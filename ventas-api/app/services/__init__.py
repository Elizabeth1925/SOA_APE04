"""
===============================================================================
ARCHIVO: app/services/__init__.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Inicializador del subpaquete `app.services`, que aloja la capa de logica
    de negocio, coordinacion distribuida en 3 fases, garantia de idempotencia
    y disparo controlado de eventos asincronos (`venta_service.py`).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Logica transaccional e idempotencia de ventas.
    - [Actividad 8]: Orquestacion segura de notificaciones RabbitMQ.
===============================================================================
"""

# [Actividad 4] [Actividad 8] Marca la carpeta `services` como subpaquete Python para la logica de negocio.
