"""
===============================================================================
ARCHIVO: app/main.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Punto de entrada ASGI de la aplicacion FastAPI (`app.main:app`). Ensambla
    la instancia web, registra los manejadores globales de excepciones RFC 7807,
    monta el enrutador REST de Compradores y Ventas (`/api/*`) y expone el
    endpoint de salud `/health` consultado por NGINX, scripts de smoke-test y
    herramientas de monitoreo.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Inicializacion del servicio REST FastAPI y
      contrato de salud `GET /health` (`{"status": "UP"}`).
    - [Actividad 4]: Conecta los manejadores de errores RFC 7807 (`errors.py`)
      y las rutas de Compradores y Ventas (`routers/ventas.py`) servidas en
      `:8002` detras del API Gateway NGINX `:8090`.
===============================================================================
"""

# [Actividad 1 / Actividad 2] [Actividad 4] Clase principal del framework FastAPI para construir la aplicacion ASGI
from fastapi import FastAPI

# [Actividad 4] [Actividad 11] Funcion que registra los exception handlers globales en formato RFC 7807
from app.errors import instalar

# [Actividad 4] Enrutador modular que agrupa los endpoints `/api/compradores` y `/api/ventas`
from app.routers.ventas import router

# [Actividad 1 / Actividad 2] [Actividad 4] Instancia principal de FastAPI publicada por Uvicorn en el puerto `:8002`
app = FastAPI(title="API de ventas de vehículos", version="1.0.0")

# [Actividad 4] [Actividad 11] Instala el manejo uniforme de errores RFC 7807 (401/403/409/422/500/503) sobre `app`
instalar(app)

# [Actividad 4] Monta las rutas del dominio de Compradores y Ventas bajo el prefijo `/api`
app.include_router(router)


# [Actividad 1 / Actividad 2] Endpoint publico de chequeo de disponibilidad del microservicio (sin requerir JWT)
@app.get("/health")
def health():
    """Retorna un indicador ligero de que el proceso FastAPI de `ventas-api` esta activo."""
    # Responde HTTP 200 OK con el estado estandarizado `UP` esperado por verificaciones de infraestructura
    return {"status": "UP"}
