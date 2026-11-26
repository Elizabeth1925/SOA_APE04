"""
===============================================================================
ARCHIVO: app/__init__.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Inicializador del paquete raiz `app` del microservicio Python `ventas-api`.
    Habilita la resolucion estructurada de imports absolutos (`app.config`,
    `app.db`, `app.models`, `app.routers`, `app.services`, `app.repositories`,
    `app.clients`) empleados por Uvicorn, Alembic y el worker Outbox.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Organizacion modular en capas limpias
      (configuracion, persistencia, clientes, repositorios, servicios, routers).
===============================================================================
"""

# [Actividad 1 / Actividad 2] Marca el directorio `app` como paquete Python importable.
# Permite resolver modulos internos como `app.main:app`, `app.models` o `app.worker`.
