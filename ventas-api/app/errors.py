"""
===============================================================================
ARCHIVO: app/errors.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Implementa el manejo global y homogeneo de errores HTTP siguiendo el
    estandar RFC 7807 (`application/problem+json`). Traduce excepciones de
    negocio (`ApiError`), validaciones Pydantic (`422`), errores HTTP genericos,
    conflictos de integridad en MySQL (`409`), fallos reales de base de datos
    (`503 BD_NO_DISPONIBLE`) y excepciones imprevistas (`500`) en respuestas
    estructuradas con identificador de trazabilidad (`traceId`).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Contrato uniforme de errores RFC 7807 para el nucleo de
      Compradores y Ventas (`ApiError`, validacion `422`, `409 CONFLICTO_DATOS`
      y `409 CLAVE_REUTILIZADA`).
    - [Actividad 11]: Manejo real de fallos sin confirmaciones ficticias ni
      filtracion de trazas internas al cliente: captura `SQLAlchemyError` como
      HTTP `503 BD_NO_DISPONIBLE` (prueba P13), `IntegrityError` como `409`,
      errores de autenticacion/RBAC `401`/`403` (P10) y rechazos `409` (P11/P12).
===============================================================================
"""

# [Actividad 11] Modulo de logging estandar para registrar stacktraces reales de fallos de BD e internos en servidor
import logging

# [Actividad 4] Generador de UUID v4 para asignar un `traceId` unico por cada respuesta de error RFC 7807
from uuid import uuid4

# [Actividad 4] Clase de respuesta JSON de FastAPI para emitir el encabezado `application/problem+json`
from fastapi.responses import JSONResponse
# [Actividad 4] Excepcion lanzada automaticamente por FastAPI cuando falla la validacion Pydantic de cuerpo, ruta o cabeceras
from fastapi.exceptions import RequestValidationError

# [Actividad 4] Excepcion HTTP base de Starlette/FastAPI para errores como 404 de ruta o 405 metodo no permitido
from starlette.exceptions import HTTPException

# [Actividad 11] Excepciones de SQLAlchemy para diferenciar conflictos de unicidad/FK (`IntegrityError`) de caidas de BD (`SQLAlchemyError`)
from sqlalchemy.exc import SQLAlchemyError, IntegrityError


# [Actividad 4] [Actividad 11] Excepcion de dominio utilizada por seguridad, routers y servicios para reportar errores controlados
class ApiError(Exception):
    """Encapsula un error controlado de negocio o integracion con estado HTTP, codigo semantico y detalle."""

    # Constructor que almacena el codigo HTTP, el codigo maquina (ej. `CLAVE_REUTILIZADA`) y el mensaje legible
    def __init__(self, status, codigo, detalle):
        """Inicializa el estado HTTP, codigo identificador y descripcion legible del error."""
        # Asigna en una sola sentencia los tres atributos consumidos por el manejador global `negocio`
        self.status, self.codigo, self.detalle = status, codigo, detalle


# [Actividad 4] [Actividad 11] Fabrica central de respuestas conformes al estandar RFC 7807 (Problem Details for HTTP APIs)
def respuesta(req, status, codigo, detalle):
    """Construye una `JSONResponse` con tipo `application/problem+json` y `traceId` unico."""
    # Genera un identificador de correlacion UUID v4 unico para rastrear este error en los registros
    trace = str(uuid4())
    # Retorna la respuesta HTTP con el codigo de estado exacto y el MIME oficial de RFC 7807
    return JSONResponse(status_code=status,
        media_type="application/problem+json", content={
            # URN unico que identifica semanticamente el tipo de problema dentro del dominio `ventas`
            "type": "urn:ventas:" + codigo.lower(),
            # Titulo corto del problema, codigo HTTP numerico y explicacion clara para el consumidor
            "title": codigo, "status": status, "detail": detalle,
            # Ruta solicitada (`instance`), codigo semantico estable y `traceId` de trazabilidad
            "instance": req.url.path, "codigo": codigo, "traceId": trace,
        })


# [Actividad 4] [Actividad 11] Registra todos los manejadores globales de excepciones sobre la instancia FastAPI
def instalar(app):
    """Conecta los exception handlers RFC 7807 a la aplicacion FastAPI."""

    # [Actividad 4] [Actividad 11] Captura errores explicitos de negocio/seguridad/integracion (401, 403, 404, 409, 422, 503)
    @app.exception_handler(ApiError)
    async def negocio(req, ex):
        """Convierte cualquier `ApiError` lanzado en la aplicacion a formato RFC 7807."""
        # Serializa exactamente el status, codigo y detalle provistos por el punto que detecto el error
        return respuesta(req, ex.status, ex.codigo, ex.detalle)

    # [Actividad 4] Captura fallos de esquema Pydantic (por ejemplo compradorId <= 0, correo invalido o falta Idempotency-Key)
    @app.exception_handler(RequestValidationError)
    async def validar(req, ex):
        """Formatea los errores de validacion de entrada de FastAPI en una respuesta RFC 7807 HTTP 422."""
        # Une cada error detectado con su ubicacion (`loc`) y descripcion (`msg`) separados por punto y coma
        return respuesta(req, 422, "VALIDACION", "; ".join(
            ".".join(map(str, e["loc"])) + ": " + e["msg"]
            for e in ex.errors()))

    # [Actividad 4] Captura excepciones HTTP estandar de Starlette/FastAPI (por ejemplo ruta inexistente 404)
    @app.exception_handler(HTTPException)
    async def http(req, ex):
        """Adapta excepciones HTTP nativas del framework al contrato uniforme RFC 7807."""
        # Preserva el codigo de estado HTTP original y encapsula el detalle como `ERROR_HTTP`
        return respuesta(req, ex.status_code, "ERROR_HTTP", str(ex.detail))

    # [Actividad 4] [Actividad 11] Captura violaciones de clave unica o foranea en MySQL (ej. comprador duplicado) -> HTTP 409
    @app.exception_handler(IntegrityError)
    async def integridad(req, ex):
        """Traduce colisiones de unicidad o integridad referencial de SQLAlchemy en HTTP 409 Conflict."""
        # Evita exponer detalles internos de SQL y responde con codigo semantico `CONFLICTO_DATOS`
        return respuesta(req, 409, "CONFLICTO_DATOS", "Registro duplicado")

    # [Actividad 11] Captura fallos reales de conectividad o ejecucion en MySQL (prueba P13) -> HTTP 503 sin simular exito
    @app.exception_handler(SQLAlchemyError)
    async def bd(req, ex):
        """Registra el fallo real de base de datos y devuelve HTTP 503 `BD_NO_DISPONIBLE`."""
        # Deja constancia tecnica completa del stacktrace en los logs del servidor para diagnostico
        logging.error("Fallo de persistencia", exc_info=(type(ex), ex, ex.__traceback__))
        # Informa al cliente que la persistencia no esta disponible en este momento (503 Service Unavailable)
        return respuesta(req, 503, "BD_NO_DISPONIBLE",
                         "No se pudo acceder a la persistencia")

    # [Actividad 11] Red de seguridad final para excepciones no controladas -> HTTP 500 sin filtrar detalles sensibles
    @app.exception_handler(Exception)
    async def inesperado(req, ex):
        """Registra cualquier excepcion inesperada y responde HTTP 500 `ERROR_INTERNO`."""
        # Escribe la traza completa en el log del microservicio para auditoria tecnica
        logging.error("Fallo inesperado", exc_info=(type(ex), ex, ex.__traceback__))
        # Devuelve un mensaje generico seguro conforme a RFC 7807 protegiendo internos del servidor
        return respuesta(req, 500, "ERROR_INTERNO", "Error interno registrado")
