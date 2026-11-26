"""
===============================================================================
ARCHIVO: app/models.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Define el mapeo objeto-relacional (ORM SQLAlchemy 2.0) para las tablas
    `compradores` y `ventas` en MySQL. Modela las restricciones de unicidad por
    usuario/documento y por usuario/clave de idempotencia, los campos de precio
    autoritativo, trazabilidad distribuida, registro de fallos reales y bandera
    de notificacion del patron Outbox simplificado.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Sincronizado columna por columna con la
      migracion inicial `alembic/versions/0001_ventas.py`.
    - [Actividad 4]: Entidades nucleo `Comprador` y `Venta`, restriccion unica
      `uq_venta_usuario_clave` para idempotencia atomica, huella `request_hash`
      SHA-256, `precio_acordado` y `moneda` fijados desde Spring Boot.
    - [Actividad 8]: Campo `notificacion` (`PENDIENTE` / `ENVIADA`) que habilita
      la guardia estricta de publicacion unica y reintentos del worker Outbox.
    - [Actividad 10 - P27]: Coherencia transversal de `comprador_id`,
      `vehiculo_id`, `id` (`ventaId`) y `operacion_id` unico en toda la cadena.
    - [Actividad 11]: Campos `estado`, `error_status` y `error_detalle` para
      auditar rechazos y fallos reales sin fabricar confirmaciones.
===============================================================================
"""

# [Actividad 4] Utilidades estandar de Python para generar marcas temporales consistentes en UTC
from datetime import datetime, timezone

# [Actividad 4] Tipo numerico exacto `Decimal` para almacenar importes monetarios sin errores de punto flotante
from decimal import Decimal

# [Actividad 1 / Actividad 2] [Actividad 4] Tipos SQL y restricciones de integridad relacional de SQLAlchemy
from sqlalchemy import String, Numeric, ForeignKey, UniqueConstraint
# [Actividad 1 / Actividad 2] [Actividad 4] Primitivas tipadas de SQLAlchemy 2.0 para columnas y relaciones ORM
from sqlalchemy.orm import Mapped, mapped_column, relationship

# [Actividad 1 / Actividad 2] Clase declarativa `Base` compartida con `alembic/env.py`
from app.db import Base


# [Actividad 4] Modelo ORM de la entidad `Comprador` mapeada a la tabla MySQL `compradores`
class Comprador(Base):
    """Representa un cliente comprador registrado por un operador o administrador."""

    # [Actividad 1 / Actividad 2] Nombre fisico exacto de la tabla en la base de datos `venta_vehiculos`
    __tablename__ = "compradores"

    # [Actividad 4] [Actividad 11] Impide que un mismo usuario registre dos compradores con el mismo documento (dispara 409)
    __table_args__ = (
        UniqueConstraint("usuario", "documento",
                         name="uq_comprador_usuario_documento"),
    )

    # [Actividad 10 - P27] Identificador primario autoincremental del comprador (`compradorId`)
    id: Mapped[int] = mapped_column(primary_key=True)

    # [Actividad 4] Usuario propietario autenticado via Spring Boot que dio de alta al comprador (usado en RBAC)
    usuario: Mapped[str] = mapped_column(String(100))

    # [Actividad 4] Numero de documento o cedula del comprador (hasta 30 caracteres)
    documento: Mapped[str] = mapped_column(String(30))

    # [Actividad 4] Nombre completo o razon social del comprador (hasta 120 caracteres)
    nombre: Mapped[str] = mapped_column(String(120))

    # [Actividad 4] Correo electronico de contacto del comprador (almacenado internamente, excluido de eventos RabbitMQ)
    correo: Mapped[str] = mapped_column(String(200))

    # [Actividad 4] [Actividad 10 - P27] Relacion ORM uno-a-muchos hacia las ventas vinculadas a este comprador
    ventas: Mapped[list["Venta"]] = relationship(
        back_populates="comprador")


# [Actividad 4] [Actividad 8] [Actividad 10 - P27] [Actividad 11] Modelo ORM de la entidad `Venta` en la tabla MySQL `ventas`
class Venta(Base):
    """Representa una transaccion distribuida de venta de vehiculo con idempotencia y estado Outbox."""

    # [Actividad 1 / Actividad 2] Nombre fisico exacto de la tabla de ventas en MySQL
    __tablename__ = "ventas"

    # [Actividad 4] [Actividad 11] Restriccion unica compuesta que garantiza idempotencia aun bajo concurrencia simultanea (P11)
    __table_args__ = (
        UniqueConstraint("usuario", "idempotency_key",
                         name="uq_venta_usuario_clave"),
    )

    # [Actividad 10 - P27] Identificador primario autoincremental de la venta (`ventaId`)
    id: Mapped[int] = mapped_column(primary_key=True)

    # [Actividad 10 - P27] UUID v4 unico que correlaciona esta venta con la reserva en Spring Boot y el evento RabbitMQ (`operacionId`)
    operacion_id: Mapped[str] = mapped_column(String(36), unique=True)

    # [Actividad 4] Nombre de usuario del vendedor autenticado que creo la venta (control de propiedad RBAC)
    usuario: Mapped[str] = mapped_column(String(100))

    # [Actividad 10 - P27] Clave foranea obligatoria hacia `compradores.id` manteniendo integridad referencial (`compradorId`)
    comprador_id: Mapped[int] = mapped_column(
        ForeignKey("compradores.id"))

    # [Actividad 10 - P27] Identificador del vehiculo gestionado en el microservicio Java (`vehiculoId`)
    vehiculo_id: Mapped[int]

    # [Actividad 4] Clave recibida en la cabecera HTTP `Idempotency-Key` (8 a 100 caracteres)
    idempotency_key: Mapped[str] = mapped_column(String(100))

    # [Actividad 4] [Actividad 11] Huella SHA-256 hexadecimal del payload canonico usada para detectar reutilizacion con cuerpo distinto (409)
    request_hash: Mapped[str] = mapped_column(String(64))

    # [Actividad 4] [Actividad 11] Estado transaccional inicializado en `PENDIENTE`; transita a `EN_REVISION`, `COMPLETADA` o `FALLIDA`
    estado: Mapped[str] = mapped_column(String(20), default="PENDIENTE")

    # [Actividad 4] UUID de la reserva devuelto por Spring Boot una vez reservado el vehiculo (nulo hasta obtener reserva)
    reserva_id: Mapped[str | None] = mapped_column(String(36))

    # [Actividad 4] [Actividad 11] Precio acordado oficial proveniente exclusivamente de Spring Boot (integridad de precios, P18)
    precio_acordado: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))

    # [Actividad 4] Codigo ISO de moneda oficial (ej. `USD`) devuelto por Spring Boot junto con la reserva
    moneda: Mapped[str | None] = mapped_column(String(3))

    # [Actividad 11] Codigo HTTP de rechazo remoto (`404`, `409`, `422`) guardado cuando la venta resulta `FALLIDA` (P12)
    error_status: Mapped[int | None]

    # [Actividad 11] Detalle real del rechazo o fallo de comunicacion con Java para diagnostico y conciliacion
    error_detalle: Mapped[str | None] = mapped_column(String(250))

    # [Actividad 8] Estado Outbox de mensajeria RabbitMQ: nace en `PENDIENTE` y cambia a `ENVIADA` solo tras publicacion exitosa
    notificacion: Mapped[str] = mapped_column(String(20), default="PENDIENTE")

    # [Actividad 4] [Actividad 10 - P27] Fecha/hora UTC naive compatible con `DATETIME` de MySQL generada al insertar
    fecha: Mapped[datetime] = mapped_column(
        default=lambda: datetime.now(timezone.utc).replace(tzinfo=None))

    # [Actividad 4] [Actividad 10 - P27] Relacion ORM muchos-a-uno hacia el `Comprador` propietario de la venta
    comprador: Mapped[Comprador] = relationship(back_populates="ventas")
