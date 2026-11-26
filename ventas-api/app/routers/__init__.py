"""
===============================================================================
ARCHIVO: app/routers/__init__.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Inicializador del subpaquete `app.routers`, que expone la capa HTTP/REST
    del microservicio `ventas-api` (endpoints de Compradores y Ventas montados
    en `app/main.py` bajo el prefijo `/api`).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Organizacion de endpoints FastAPI `/api/compradores` y
      `/api/ventas` servidos en `:8002` detras de NGINX `:8090`.
===============================================================================
"""

# [Actividad 4] Marca la carpeta `routers` como subpaquete Python para los controladores REST de FastAPI.
