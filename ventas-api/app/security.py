# Cliente HTTP sincrono para consultar el servicio de identidad en Java
import httpx

# Inyeccion de encabezados HTTP en FastAPI
from fastapi import Header

# URL base del servicio de Java configurado en .env
from app.config import JAVA_URL
# Excepcion personalizada de error de negocio para responder con Problem Details
from app.errors import ApiError

# Dependencia de seguridad de FastAPI para validar el token Bearer contra el servicio de identidad de Java
def identidad(authorization: str | None = Header(default=None)):
    # Valida la presencia del encabezado Authorization y el prefijo Bearer
    if not authorization or not authorization.startswith("Bearer "):
        raise ApiError(401, "NO_AUTENTICADO", "Enviar access token")

    try:
        # Envia una peticion GET al endpoint de identidad de Java con un timeout de 5 segundos
        r = httpx.get(JAVA_URL + "/api/identidad",
                      headers={"Authorization": authorization}, timeout=5)
    except httpx.RequestError:
        # Si el servicio de Java esta caido o no responde a tiempo, se lanza un 503
        raise ApiError(503, "IDENTIDAD_NO_DISPONIBLE", "Java no responde")

    # Si Java rechaza las credenciales por token invalido o expirado
    if r.status_code in (401, 403):
        raise ApiError(r.status_code, "ACCESO_RECHAZADO", "Token no autorizado")

    # Si Java responde con cualquier otro codigo diferente a HTTP 200 OK
    if r.status_code != 200:
        raise ApiError(503, "IDENTIDAD_NO_DISPONIBLE", "No se validó la identidad")

    try:
        # Deserializa la respuesta en formato JSON
        data = r.json()
        # Verifica que la respuesta sea un diccionario/objeto JSON
        if not isinstance(data, dict):
            raise ValueError("No es un objeto")
        # Verifica que contenga el campo 'usuario' de tipo cadena
        if not isinstance(data.get("usuario"), str):
            raise ValueError("Usuario ausente")
        # Verifica que contenga la lista de 'roles'
        if not isinstance(data.get("roles"), list):
            raise ValueError("Roles ausentes")
    except (ValueError, TypeError):
        # Si el JSON recibido no tiene el formato esperado
        raise ApiError(503, "IDENTIDAD_INVALIDA", "Respuesta de Java inválida")

    # Proteccion contra nombres de usuario desbordados
    if len(data["usuario"]) > 100:
        raise ApiError(403, "IDENTIDAD_INVALIDA", "Usuario demasiado largo")

    # Retorna la informacion del usuario autenticado (nombre y roles)
    return data

# Valida que el usuario posea el rol de operador o administrador
def vendedor(usuario):
    # Verifica la interseccion entre los roles requeridos y los roles del usuario autenticado
    if not {"ROLE_OPERADOR", "ROLE_ADMIN"}.intersection(usuario["roles"]):
        raise ApiError(403, "SIN_PERMISO", "Se requiere OPERADOR o ADMIN")

# Valida que el usuario sea el dueno del recurso o un administrador con privilegios globales
def propietario(usuario, dueno):
    # Comprueba si el usuario autenticado no coincide con el propietario y tampoco es ADMIN
    if usuario["usuario"] != dueno and "ROLE_ADMIN" not in usuario["roles"]:
        raise ApiError(403, "SIN_PERMISO", "Recurso de otro usuario")
