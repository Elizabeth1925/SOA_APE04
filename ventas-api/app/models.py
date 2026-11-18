# Modulos estandar para manejo de fechas y zona horaria UTC
from datetime import datetime, timezone

# Modulo para manejo preciso de cantidades monetarias sin perdida de precision
from decimal import Decimal

# Tipos de columnas y restricciones relacionales de SQLAlchemy
from sqlalchemy import String, Numeric, ForeignKey, UniqueConstraint
# Utilidades modernas de SQLAlchemy 2.0 para mapeo declarativo tipado y relaciones entre tablas
from sqlalchemy.orm import Mapped, mapped_column, relationship

# Importa la clase base del ORM definida en app.db
from app.db import Base

# Modelo ORM que representa la tabla 'compradores' en la base de datos
class Comprador(Base):
    # Nombre exacto de la tabla en la base de datos MySQL
    __tablename__ = "compradores"

    # Restricciones a nivel de tabla: un usuario no puede registrar dos veces el mismo documento de comprador
    __table_args__ = (
        UniqueConstraint("usuario", "documento",
                         name="uq_comprador_usuario_documento"),
    )

    # Identificador primario autoincremental del comprador
    id: Mapped[int] = mapped_column(primary_key=True)

    # Nombre de usuario del vendedor o propietario que registro este comprador (maximo 100 caracteres)
    usuario: Mapped[str] = mapped_column(String(100))

    # Numero de documento de identidad del comprador (cedula, pasaporte, RUC, etc., maximo 30 caracteres)
    documento: Mapped[str] = mapped_column(String(30))

    # Nombre completo o razon social del comprador (maximo 120 caracteres)
    nombre: Mapped[str] = mapped_column(String(120))

    # Correo electronico de contacto del comprador para enviar notificaciones (maximo 200 caracteres)
    correo: Mapped[str] = mapped_column(String(200))

    # Relacion uno-a-muchos: un comprador puede tener multiples ventas asociadas
    ventas: Mapped[list["Venta"]] = relationship(
        back_populates="comprador")

# Modelo ORM que representa la tabla 'ventas' en la base de datos
class Venta(Base):
    # Nombre de la tabla de ventas
    __tablename__ = "ventas"

    # Restricciones a nivel de tabla: garantiza la idempotencia combinando usuario y clave de idempotencia
    __table_args__ = (
        UniqueConstraint("usuario", "idempotency_key",
                         name="uq_venta_usuario_clave"),
    )

    # Clave primaria autoincremental de la venta
    id: Mapped[int] = mapped_column(primary_key=True)

    # Identificador unico de la operacion distribuida (UUID v4 de 36 caracteres) para conciliar con Java
    operacion_id: Mapped[str] = mapped_column(String(36), unique=True)

    # Nombre del usuario que inicio la transaccion de venta
    usuario: Mapped[str] = mapped_column(String(100))

    # Clave foranea hacia la tabla 'compradores'
    comprador_id: Mapped[int] = mapped_column(
        ForeignKey("compradores.id"))

    # Identificador del vehiculo en el microservicio externo de Java
    vehiculo_id: Mapped[int]

    # Clave de idempotencia enviada por el cliente en el encabezado HTTP
    idempotency_key: Mapped[str] = mapped_column(String(100))

    # Hash SHA-256 del cuerpo de la peticion para verificar que la peticion reenviada no haya sido modificada
    request_hash: Mapped[str] = mapped_column(String(64))

    # Estado actual de la venta: PENDIENTE, EN_REVISION, COMPLETADA o FALLIDA
    estado: Mapped[str] = mapped_column(String(20), default="PENDIENTE")

    # Identificador de la reserva generado por el microservicio en Java (UUID v4 de 36 caracteres)
    reserva_id: Mapped[str | None] = mapped_column(String(36))

    # Precio final acordado del vehiculo (12 digitos en total, 2 decimales)
    precio_acordado: Mapped[Decimal | None] = mapped_column(Numeric(12, 2))

    # Codigo ISO de la moneda (por ejemplo: USD, EUR, etc., 3 caracteres)
    moneda: Mapped[str | None] = mapped_column(String(3))

    # Codigo de estado HTTP recibido si ocurrio un error al comunicarse con Java
    error_status: Mapped[int | None]

    # Mensaje o descripcion del error si la operacion de venta fallo
    error_detalle: Mapped[str | None] = mapped_column(String(250))

    # Estado del envio de la notificacion: PENDIENTE o ENVIADA
    notificacion: Mapped[str] = mapped_column(String(20), default="PENDIENTE")

    # Fecha y hora en UTC en que se creo el registro de venta (sin informacion de zona para MySQL)
    fecha: Mapped[datetime] = mapped_column(
        default=lambda: datetime.now(timezone.utc).replace(tzinfo=None))

    # Relacion inversa hacia el modelo Comprador
    comprador: Mapped[Comprador] = relationship(back_populates="ventas")
