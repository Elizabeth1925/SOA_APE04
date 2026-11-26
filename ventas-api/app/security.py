"""
===============================================================================
ARCHIVO: app/security.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Implementa la capa de seguridad delegada y control de acceso basado en
    roles (RBAC) del microservicio `ventas-api`. En lugar de duplicar secretos
    de firma JWT o tablas de usuarios en Python, delega la validacion del token
    Bearer hacia el microservicio autoritativo en Java (`GET /api/identidad` a
    traves de `JAVA_URL`, normalmente NGINX :8090) y aplica guardias de rol y
    propiedad sobre compradores y ventas.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Autenticacion delegada mediante dependencia FastAPI
      `identidad()` consultando `GET /api/identidad` en Java, mas guardias
      RBAC `vendedor()` (`ROLE_OPERADOR` o `ROLE_ADMIN`) y `propietario()`
      (dueno del recurso o `ROLE_ADMIN`).
    - [Actividad 11]: Manejo estricto y real de fallos de seguridad e
      infraestructura sin otorgar accesos ficticios: `401 NO_AUTENTICADO`,
      `401`/`403 ACCESO_RECHAZADO`, `403 SIN_PERMISO` (prueba P10) y
      `503 IDENTIDAD_NO_DISPONIBLE` / `IDENTIDAD_INVALIDA` si Java cae (P13).
===============================================================================
"""

# [Actividad 4] Cliente HTTP sincrono utilizado para delegar la validacion del token Bearer hacia Java
import httpx

# [Actividad 4] Extractor de cabeceras HTTP de FastAPI para leer `Authorization` en cada peticion protegida
from fastapi import Header

# [Actividad 1 / Actividad 2] [Actividad 4] URL base hacia NGINX/Spring Boot leida desde configuracion de entorno
from app.config import JAVA_URL
# [Actividad 4] [Actividad 11] Excepcion controlada que produce respuestas de error RFC 7807
from app.errors import ApiError


# [Actividad 4] [Actividad 11] Dependencia FastAPI inyectada con `Depends(identidad)` en todos los endpoints protegidos
def identidad(authorization: str | None = Header(default=None)):
    """Valida el token Bearer contra `GET /api/identidad` en Java y retorna el perfil autenticado."""
    # [Actividad 4] [Actividad 11] Rechaza de inmediato peticiones sin cabecera `Authorization` o sin esquema `Bearer ` (P10 -> 401)
    if not authorization or not authorization.startswith("Bearer "):
        # Emite HTTP 401 RFC 7807 sin consultar servicios internos
        raise ApiError(401, "NO_AUTENTICADO", "Enviar access token")

    # [Actividad 4] [Actividad 11] Invoca el endpoint central de introspeccion de identidad en Java con timeout acotado
    try:
        # Reenvia el encabezado `Authorization` recibido para que Spring Security valide firma, expiracion y revocacion del JWT
        r = httpx.get(JAVA_URL + "/api/identidad",
                      headers={"Authorization": authorization}, timeout=5)
    # [Actividad 11] Captura errores de red, timeout o caida de NGINX/Java (P13 -> 503 real, fail-closed)
    except httpx.RequestError:
        # Falla cerrado con HTTP 503 para nunca conceder acceso cuando el proveedor de identidad esta caido
        raise ApiError(503, "IDENTIDAD_NO_DISPONIBLE", "Java no responde")

    # [Actividad 4] [Actividad 11] Si Java rechaza el JWT por invalido, expirado o sin permisos (P10 -> 401/403)
    if r.status_code in (401, 403):
        # Propaga el mismo codigo HTTP de rechazo (`401` o `403`) envuelto en RFC 7807
        raise ApiError(r.status_code, "ACCESO_RECHAZADO", "Token no autorizado")

    # [Actividad 11] Si Java responde cualquier codigo anomalo distinto de 200 OK (ej. 500 o 502)
    if r.status_code != 200:
        # Retorna HTTP 503 indicando indisponibilidad real del servicio de identidad
        raise ApiError(503, "IDENTIDAD_NO_DISPONIBLE", "No se validó la identidad")

    # [Actividad 4] [Actividad 11] Valida defensivamente la estructura del JSON retornado por Java
    try:
        # Parsea el cuerpo JSON con los atributos de identidad (`usuario`, `roles`, `rol`, etc.)
        data = r.json()
        # Asegura que el payload raiz sea un objeto JSON (diccionario)
        if not isinstance(data, dict):
            # Fuerza entrada al bloque `except` si el payload no es un objeto
            raise ValueError("No es un objeto")
        # Asegura que el nombre de usuario este presente y sea una cadena valida
        if not isinstance(data.get("usuario"), str):
            # Rechaza identidades malformadas sin `usuario`
            raise ValueError("Usuario ausente")
        # Asegura que la coleccion de roles venga como lista para las comprobaciones RBAC
        if not isinstance(data.get("roles"), list):
            # Rechaza identidades sin lista de roles
            raise ValueError("Roles ausentes")
    # [Actividad 11] Captura respuestas corruptas, no JSON o incompletas provenientes de Java/gateway
    except (ValueError, TypeError):
        # Responde HTTP 503 fail-closed evitando aceptar una identidad inconsistente
        raise ApiError(503, "IDENTIDAD_INVALIDA", "Respuesta de Java inválida")

    # [Actividad 4] Protege la columna `usuario` (VARCHAR 100 en MySQL) frente a valores desbordados
    if len(data["usuario"]) > 100:
        # Rechaza con HTTP 403 cualquier identidad que exceda el limite del modelo relacional
        raise ApiError(403, "IDENTIDAD_INVALIDA", "Usuario demasiado largo")

    # [Actividad 4] Retorna el diccionario validado de identidad para uso en routers y guardias RBAC
    return data


# [Actividad 4] [Actividad 11] Guardia RBAC que restringe operaciones de escritura (crear comprador/venta) a vendedores autorizados
def vendedor(usuario):
    """Exige que el usuario autenticado posea el rol `ROLE_OPERADOR` o `ROLE_ADMIN` (P10 -> 403)."""
    # Comprueba si existe al menos un rol permitido dentro de `usuario["roles"]` (por ejemplo bloquea `ROLE_CONSULTA`)
    if not {"ROLE_OPERADOR", "ROLE_ADMIN"}.intersection(usuario["roles"]):
        # Deniega el acceso con HTTP 403 `SIN_PERMISO` en formato RFC 7807
        raise ApiError(403, "SIN_PERMISO", "Se requiere OPERADOR o ADMIN")


# [Actividad 4] [Actividad 11] Guardia RBAC de propiedad horizontal para evitar acceso a recursos de terceros (IDOR)
def propietario(usuario, dueno):
    """Verifica que el usuario autenticado sea el dueno del recurso o posea privilegio global `ROLE_ADMIN`."""
    # Rechaza si el nombre de usuario autenticado difiere del propietario del comprador/venta y no es administrador
    if usuario["usuario"] != dueno and "ROLE_ADMIN" not in usuario["roles"]:
        # Bloquea la operacion con HTTP 403 `SIN_PERMISO` (P10)
        raise ApiError(403, "SIN_PERMISO", "Recurso de otro usuario")
