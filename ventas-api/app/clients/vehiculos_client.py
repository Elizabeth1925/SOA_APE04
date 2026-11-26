"""
===============================================================================
ARCHIVO: app/clients/vehiculos_client.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Adaptador HTTP autenticado servicio-a-servicio para comunicarse con el
    microservicio de Vehiculos en Spring Boot (a traves de `JAVA_URL`, por
    defecto NGINX :8090). Gestiona las operaciones distribuidas de consulta,
    reserva y confirmacion de vehiculos mediante `X-Service-Key` y timeouts
    estrictos, devolviendo el precio y moneda autoritativos definidos en Java.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Integridad estricta de precios consultados desde Spring
      Boot (al reservar o consultar por `operacionId`, Java devuelve el precio
      oficial del inventario que `venta_service.py` persiste), autenticacion
      inter-servicio `X-Service-Key` y protocolo en dos pasos
      reservar -> confirmar.
    - [Actividad 10 - P27]: Propaga `operacionId` hacia `/api/reservas` y
      `/api/vehiculos/{vehiculo}/reservas`, manteniendo coherencia transversal
      entre el registro de venta en Python y la reserva/venta en Java.
    - [Actividad 11]: Encapsula fallos reales de red (`503`), rechazos HTTP
      (`404`, `409`, `422`) y respuestas corruptas (`502`) en `FalloRemoto`
      para que `venta_service.py` nunca fabrique confirmaciones ficticias
      (pruebas P12, P13, P18).
===============================================================================
"""

# [Actividad 4] Cliente HTTP sincrono con soporte de timeouts diferenciados de conexion y lectura
import httpx

# [Actividad 1 / Actividad 2] [Actividad 4] URL base de Java/NGINX y clave compartida de servicio desde `.env`
from app.config import JAVA_URL, SERVICE_KEY


# [Actividad 4] [Actividad 11] Excepcion tecnica de integracion remota que separa errores HTTP/red de excepciones de dominio
class FalloRemoto(Exception):
    """Representa un fallo de transporte, rechazo HTTP (>=400) o respuesta ilegible de Spring Boot."""

    # Almacena el codigo HTTP asociado (real o sintetico 502/503) y la descripcion tecnica del fallo
    def __init__(self, status, detalle):
        """Inicializa el codigo HTTP y detalle del fallo remoto."""
        # Asigna ambos atributos para que `venta_service.procesar()` decida entre `FALLIDA` o `EN_REVISION`
        self.status, self.detalle = status, detalle


# [Actividad 4] [Actividad 10 - P27] [Actividad 11] Cliente HTTP hacia los endpoints internos de reservas de Spring Boot
class VehiculosClient:
    """Encapsula las llamadas REST autenticadas con `X-Service-Key` hacia `vehiculos-api` (Java)."""

    # [Actividad 4] [Actividad 11] Despachador comun de solicitudes HTTP con autenticacion de servicio y timeouts estrictos
    def llamar(self, metodo, ruta, **kwargs):
        """Ejecuta una llamada HTTP hacia `JAVA_URL + ruta` y retorna el JSON parseado o lanza `FalloRemoto`."""
        # [Actividad 11] Bloque de transporte protegido contra caidas de red, DNS o timeouts de Spring Boot/NGINX
        try:
            # Envia la peticion inyectando `X-Service-Key` y acotando la espera (2s conexion, 5s total)
            r = httpx.request(metodo, JAVA_URL + ruta,
                headers={"X-Service-Key": SERVICE_KEY},
                timeout=httpx.Timeout(5, connect=2), **kwargs)
        # [Actividad 11] Ante corte de red o timeout, el resultado en Java es incierto -> `503` para dejar en `EN_REVISION` (P13)
        except httpx.RequestError:
            # Nunca asume exito ni fracaso definitivo cuando hay duda de red; delega conciliacion al worker
            raise FalloRemoto(503, "Resultado remoto desconocido")

        # [Actividad 4] [Actividad 11] Cualquier respuesta HTTP 4xx/5xx de Java se eleva conservando el codigo exacto (P12 -> 404/409/422)
        if r.status_code >= 400:
            # Permite a `venta_service.procesar()` distinguir rechazos definitivos (404/409/422) de fallos 5xx
            raise FalloRemoto(r.status_code, "Operación Java rechazada")

        # [Actividad 4] [Actividad 11] Deserializa la carga util JSON (que contiene `id`, `estado`, `precio` oficial y `moneda`)
        try:
            # Si hay cuerpo en la respuesta, parsea el JSON; si el cuerpo esta vacio (ej. 204), retorna `None`
            return r.json() if r.content else None
        # [Actividad 11] Protege contra respuestas HTML/corruptas (ej. pagina de error de proxy mal formateada)
        except ValueError:
            # Clasifica la respuesta invalida como HTTP 502 Bad Gateway remoto
            raise FalloRemoto(502, "Respuesta Java ilegible")

    # [Actividad 4] [Actividad 10 - P27] Consulta idempotente en Java para verificar si ya existe reserva para `operacionId`
    def por_operacion(self, op):
        """Busca en Spring Boot (`GET /api/reservas?operacionId=...`) una reserva ya creada para la operacion."""
        # Permite recuperar el estado real y el precio oficial tras un timeout o reintento del worker
        return self.llamar("GET", "/api/reservas",
                           params={"operacionId": op})

    # [Actividad 4] [Actividad 10 - P27] [Actividad 11] Crea la reserva del vehiculo y obtiene su precio/moneda oficiales desde Java (P18)
    def reservar(self, vehiculo, op):
        """Solicita en Spring Boot (`POST /api/vehiculos/{vehiculo}/reservas`) reservar el vehiculo para `operacionId`."""
        # Envia unicamente `operacionId` sin precio del cliente: Spring Boot fija el precio autoritativo del catalogo
        return self.llamar("POST", f"/api/vehiculos/{vehiculo}/reservas",
                           json={"operacionId": op})

    # [Actividad 4] Confirma definitivamente la reserva en Java cambiando el estado del vehiculo a vendido/confirmado
    def confirmar(self, reserva):
        """Ejecuta `PUT /api/reservas/{reserva}/confirmacion` en Spring Boot para cerrar la reserva."""
        # Retorna la reserva confirmada (`estado == "CONFIRMADA"`) o lanza `FalloRemoto` si falla
        return self.llamar("PUT", f"/api/reservas/{reserva}/confirmacion")
