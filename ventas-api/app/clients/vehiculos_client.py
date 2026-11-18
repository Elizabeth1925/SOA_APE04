# Cliente HTTP para realizar peticiones de red hacia servicios externos
import httpx

# Importa la URL base y la clave de autenticacion de servicio para el microservicio de Java
from app.config import JAVA_URL, SERVICE_KEY

# Excepcion especializada para capturar y encapsular fallos en las llamadas remotas
class FalloRemoto(Exception):
    # Inicializa el error con el codigo HTTP devuelto y el detalle explicativo
    def __init__(self, status, detalle):
        self.status, self.detalle = status, detalle

# Cliente HTTP encargado de la comunicacion con el microservicio de Vehiculos en Java
class VehiculosClient:
    # Metodo centralizado para despachar peticiones HTTP autenticadas con timeouts estrictos
    def llamar(self, metodo, ruta, **kwargs):
        try:
            # Ejecuta la peticion agregando la clave de servicio 'X-Service-Key'
            # Timeout total de 5 segundos, timeout de conexion de 2 segundos
            r = httpx.request(metodo, JAVA_URL + ruta,
                headers={"X-Service-Key": SERVICE_KEY},
                timeout=httpx.Timeout(5, connect=2), **kwargs)
        except httpx.RequestError:
            # Captura errores de red, DNS o caida del servicio Java devolviendo estado 503
            raise FalloRemoto(503, "Resultado remoto desconocido")

        # Si el servicio responde con codigos de error del cliente o del servidor (>= 400)
        if r.status_code >= 400:
            raise FalloRemoto(r.status_code, "Operación Java rechazada")

        try:
            # Retorna el cuerpo JSON parseado si hay contenido, de lo contrario None
            return r.json() if r.content else None
        except ValueError:
            # Si el servidor responde 200 pero el contenido no es JSON valido (HTML, texto plano corrompido, etc.)
            raise FalloRemoto(502, "Respuesta Java ilegible")

    # Consulta una reserva existente en Java usando el ID de operacion idempotente
    def por_operacion(self, op):
        return self.llamar("GET", "/api/reservas",
                           params={"operacionId": op})

    # Solicita la creacion de una reserva de vehiculo asociada a la operacion dada
    def reservar(self, vehiculo, op):
        return self.llamar("POST", f"/api/vehiculos/{vehiculo}/reservas",
                           json={"operacionId": op})

    # Confirma de forma definitiva la reserva previamente realizada
    def confirmar(self, reserva):
        return self.llamar("PUT", f"/api/reservas/{reserva}/confirmacion")
