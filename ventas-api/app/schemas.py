"""
===============================================================================
ARCHIVO: app/schemas.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Define los esquemas DTO (Data Transfer Objects) con Pydantic v2 para
    validar y normalizar las solicitudes REST entrantes de `POST /api/compradores`
    y `POST /api/ventas`. Soporta interoperabilidad entre clientes heterogeneos
    de la arquitectura SOA (PWA React en camelCase, Windows Forms y scripts en
    snake_case o alias en espanol) manteniendo reglas estrictas de validacion.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Contratos REST iniciales de entrada para
      creacion de compradores y ventas con respuesta HTTP 422 ante datos invalidos.
    - [Actividad 4]: Validacion de campos obligatorios y normalizacion de alias
      (`documento`/`cedula`, `correo`/`email`, `vehiculoId`/`vehiculo_id`,
      `compradorId`/`comprador_id`). Acepta opcionalmente `precio` en el payload
      de entrada para compatibilidad con clientes y huella de idempotencia, pero
      la integridad estricta de precios en `venta_service.py` siempre impone el
      precio oficial consultado desde Spring Boot.
    - [Actividad 10 - P27]: Valida que `vehiculoId` y `compradorId` sean
      enteros estrictamente positivos (> 0) preservando coherencia transversal.
    - [Actividad 11]: Rechaza payloads invalidos en la frontera HTTP (422) y
      neutraliza manipulaciones del campo `precio` desde el cliente (P18).
===============================================================================
"""

# [Actividad 1 / Actividad 2] Tipo `Optional` para declarar campos opcionales o alias alternativos en los DTO
from typing import Optional
# [Actividad 1 / Actividad 2] [Actividad 4] Primitivas de Pydantic v2 para definir modelos, configuracion, restricciones y validadores de modelo
from pydantic import BaseModel, ConfigDict, Field, model_validator


# [Actividad 1 / Actividad 2] [Actividad 4] Esquema DTO de entrada para `POST /api/compradores`
class CrearComprador(BaseModel):
    """Valida y normaliza los datos de registro de un nuevo comprador."""

    # [Actividad 4] Ignora campos extra no declarados y recorta espacios al inicio/final de todas las cadenas
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    # [Actividad 4] Numero de documento canonico del comprador (opcional si el cliente envia el alias `cedula`)
    documento: Optional[str] = None
    # [Actividad 4] Alias alternativo de `documento` admitido para interoperabilidad con clientes en espanol (PWA / WinForms)
    cedula: Optional[str] = None
    # [Actividad 4] Nombre completo obligatorio del comprador con longitud controlada entre 2 y 120 caracteres
    nombre: str = Field(min_length=2, max_length=120)
    # [Actividad 4] Direccion de correo canonica del comprador (opcional si el cliente envia el alias `email`)
    correo: Optional[str] = None
    # [Actividad 4] Alias alternativo de `correo` admitido para clientes que usan nomenclatura en ingles
    email: Optional[str] = None
    # [Actividad 4] Campo opcional de telefono aceptado en la entrada para compatibilidad con formularios cliente
    telefono: Optional[str] = None

    # [Actividad 4] Validador post-parseo que unifica alias y verifica reglas semanticas obligatorias
    @model_validator(mode="after")
    def normalizar_campos(self):
        """Mapea `cedula` -> `documento` y `email` -> `correo`, validando longitud minima y formato basico."""
        # Si no se proporciono `documento`, copia el valor enviado en el alias `cedula`
        if not self.documento and self.cedula:
            # Unifica el dato hacia el atributo canonico persistido en `Comprador.documento`
            self.documento = self.cedula
        # Si no se proporciono `correo`, copia el valor enviado en el alias `email`
        if not self.correo and self.email:
            # Unifica el dato hacia el atributo canonico persistido en `Comprador.correo`
            self.correo = self.email
        # Exige que el documento normalizado exista y tenga al menos 3 caracteres utiles
        if not self.documento or len(self.documento.strip()) < 3:
            # Dispara un `RequestValidationError` traducido por `errors.py` a HTTP 422 `VALIDACION`
            raise ValueError("El documento o cedula debe tener al menos 3 caracteres")
        # Exige presencia de correo y verificacion basica del caracter `@`
        if not self.correo or "@" not in self.correo:
            # Rechaza correos ausentes o mal formados con HTTP 422 `VALIDACION`
            raise ValueError("Debe proporcionar un correo electronico valido")
        # Devuelve la instancia normalizada para su consumo directo en `routers/ventas.py`
        return self


# [Actividad 1 / Actividad 2] [Actividad 4] [Actividad 10 - P27] Esquema DTO de entrada para `POST /api/ventas`
class CrearVenta(BaseModel):
    """Valida y normaliza los identificadores requeridos para iniciar una transaccion de venta."""

    # [Actividad 4] Ignora atributos desconocidos adicionales enviados por clientes externos sin romper compatibilidad
    model_config = ConfigDict(extra="ignore")

    # [Actividad 10 - P27] Identificador del vehiculo en camelCase (contrato REST principal con PWA y Java)
    vehiculoId: Optional[int] = None
    # [Actividad 4] Alias en snake_case de `vehiculoId` para clientes Python o integraciones alternativas
    vehiculo_id: Optional[int] = None
    # [Actividad 10 - P27] Identificador del comprador en camelCase (contrato REST principal)
    compradorId: Optional[int] = None
    # [Actividad 4] Alias en snake_case de `compradorId` para compatibilidad entre clientes
    comprador_id: Optional[int] = None
    # [Actividad 4] [Actividad 11] Campo opcional tolerado en el cuerpo JSON (participa en el hash de idempotencia,
    # pero NUNCA se usa como precio real: `venta_service.py` impone estrictamente el precio devuelto por Spring Boot, P18)
    precio: Optional[float] = None

    # [Actividad 4] [Actividad 10 - P27] Validador post-parseo que unifica camelCase/snake_case y verifica IDs positivos
    @model_validator(mode="after")
    def normalizar_campos(self):
        """Unifica alias snake_case hacia camelCase y valida que `vehiculoId` y `compradorId` sean > 0."""
        # Si `vehiculoId` no fue provisto pero si `vehiculo_id`, normaliza al atributo canonico `vehiculoId`
        if self.vehiculoId is None and self.vehiculo_id is not None:
            # Garantiza que `venta_service.py` siempre lea `datos.vehiculoId`
            self.vehiculoId = self.vehiculo_id
        # Si `compradorId` no fue provisto pero si `comprador_id`, normaliza al atributo canonico `compradorId`
        if self.compradorId is None and self.comprador_id is not None:
            # Garantiza que `venta_service.py` siempre lea `datos.compradorId`
            self.compradorId = self.comprador_id
        # Verifica que `vehiculoId` exista y sea un entero estrictamente positivo
        if self.vehiculoId is None or self.vehiculoId <= 0:
            # Lanza error de validacion traducido a HTTP 422 `VALIDACION`
            raise ValueError("vehiculoId debe ser un entero estrictamente mayor a 0")
        # Verifica que `compradorId` exista y sea un entero estrictamente positivo
        if self.compradorId is None or self.compradorId <= 0:
            # Lanza error de validacion traducido a HTTP 422 `VALIDACION`
            raise ValueError("compradorId debe ser un entero estrictamente mayor a 0")
        # Retorna el DTO normalizado listo para calcular la huella SHA-256 y ejecutar la venta
        return self
