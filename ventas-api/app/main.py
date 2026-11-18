# Modulo principal del framework FastAPI para instanciar la aplicacion web
from fastapi import FastAPI

# Instalador centralizado de manejadores globales de excepciones
from app.errors import instalar

# Enrutador que contiene los endpoints del microservicio de ventas
from app.routers.ventas import router

# Instancia de la aplicacion principal de FastAPI con metadatos descriptivos
app = FastAPI(title="API de ventas de vehículos", version="1.0.0")

# Registra los manejadores de excepciones y respuestas de error personalizadas
instalar(app)

# Incluye las rutas del modulo de ventas en la aplicacion
app.include_router(router)

# Endpoint de chequeo de salud (health check) para monitoreo del estado del servicio
@app.get("/health")
def health():
    # Retorna un estado HTTP 200 con JSON indicando que el servicio esta arriba y operativo
    return {"status": "UP"}
