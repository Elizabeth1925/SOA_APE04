# Modulo para registro de logs de eventos, advertencias y errores del sistema
import logging

# Generador de identificadores unicos universales (UUID v4) para trazabilidad de errores
from uuid import uuid4

# Respuestas en formato JSON y manejo de errores de validacion de FastAPI
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError

# Excepciones estandar HTTP de Starlette/FastAPI
from starlette.exceptions import HTTPException

# Excepciones de base de datos lanzadas por SQLAlchemy
from sqlalchemy.exc import SQLAlchemyError, IntegrityError

# Excepcion personalizada para representar errores de logica de negocio de la API
class ApiError(Exception):
    # Constructor que recibe el codigo de estado HTTP, codigo descriptivo y detalle
    def __init__(self, status, codigo, detalle):
        self.status, self.codigo, self.detalle = status, codigo, detalle

# Funcion auxiliar para construir respuestas de error estandarizadas basadas en RFC 7807 (Problem Details)
def respuesta(req, status, codigo, detalle):
    # Genera un codigo de traza unico para correlacionar con logs
    trace = str(uuid4())
    # Retorna la respuesta HTTP con tipo de contenido 'application/problem+json'
    return JSONResponse(status_code=status,
        media_type="application/problem+json", content={
            "type": "urn:ventas:" + codigo.lower(),
            "title": codigo, "status": status, "detail": detalle,
            "instance": req.url.path, "codigo": codigo, "traceId": trace,
        })

# Registra los manejadores de excepciones globales sobre la aplicacion FastAPI
def instalar(app):
    # Manejador para errores de logica de negocio (ApiError)
    @app.exception_handler(ApiError)
    async def negocio(req, ex):
        return respuesta(req, ex.status, ex.codigo, ex.detalle)

    # Manejador para errores cuando los datos de la peticion no cumplen las validaciones del esquema (HTTP 422)
    @app.exception_handler(RequestValidationError)
    async def validar(req, ex):
        # Concatena los errores indicando la ruta del campo fallido y el mensaje de error
        return respuesta(req, 422, "VALIDACION", "; ".join(
            ".".join(map(str, e["loc"])) + ": " + e["msg"]
            for e in ex.errors()))

    # Manejador para excepciones HTTP genericas de Starlette/FastAPI
    @app.exception_handler(HTTPException)
    async def http(req, ex):
        return respuesta(req, ex.status_code, "ERROR_HTTP", str(ex.detail))

    # Manejador para violaciones de integridad en la BD (claves duplicadas, claves foraneas) (HTTP 409)
    @app.exception_handler(IntegrityError)
    async def integridad(req, ex):
        return respuesta(req, 409, "CONFLICTO_DATOS", "Registro duplicado")

    # Manejador para errores generales de conexion o persistencia en SQLAlchemy (HTTP 503)
    @app.exception_handler(SQLAlchemyError)
    async def bd(req, ex):
        # Registra la traza del error en el log del servidor
        logging.error("Fallo de persistencia", exc_info=(type(ex), ex, ex.__traceback__))
        return respuesta(req, 503, "BD_NO_DISPONIBLE",
                         "No se pudo acceder a la persistencia")

    # Manejador para cualquier excepcion imprevista no capturada previamente (HTTP 500)
    @app.exception_handler(Exception)
    async def inesperado(req, ex):
        # Registra en log el error critico del sistema
        logging.error("Fallo inesperado", exc_info=(type(ex), ex, ex.__traceback__))
        return respuesta(req, 500, "ERROR_INTERNO", "Error interno registrado")
