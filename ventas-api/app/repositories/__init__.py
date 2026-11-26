"""
===============================================================================
ARCHIVO: app/repositories/__init__.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Inicializador del subpaquete `app.repositories`, que encapsula las
    consultas SQLAlchemy sobre MySQL separando la persistencia de la logica
    de negocio y de los controladores HTTP.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Consultas de idempotencia y bloqueo pesimista `FOR UPDATE`.
    - [Actividad 8]: Consulta de ventas pendientes para el worker Outbox.
===============================================================================
"""

# [Actividad 4] [Actividad 8] Marca la carpeta `repositories` como subpaquete Python de acceso a datos.
