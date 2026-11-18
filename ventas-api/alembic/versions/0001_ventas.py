# Operaciones de esquema DDL proporcionadas por Alembic
from alembic import op

# Tipos de datos de SQLAlchemy para definicion de columnas
import sqlalchemy as sa

# Identificador unico de esta revision de migracion
revision = "0001"

# Identificador de la revision previa (None porque es la migracion inicial)
down_revision = None

# Etiquetas de rama para control de ramificaciones en migraciones
branch_labels = None

# Dependencias entre ramas de migraciones
depends_on = None

# Funcion que aplica los cambios en el esquema de base de datos hacia adelante
def upgrade():
    # Creacion de la tabla 'compradores'
    op.create_table("compradores",
        # Columna de identificador primario autoincremental
        sa.Column("id", sa.Integer, primary_key=True),
        # Usuario del vendedor que registro al comprador
        sa.Column("usuario", sa.String(100), nullable=False),
        # Documento unico por usuario
        sa.Column("documento", sa.String(30), nullable=False),
        # Nombre del comprador
        sa.Column("nombre", sa.String(120), nullable=False),
        # Correo para envio de notificaciones
        sa.Column("correo", sa.String(200), nullable=False),
        # Restriccion de unicidad: un usuario no puede duplicar documentos de compradores
        sa.UniqueConstraint("usuario", "documento",
                            name="uq_comprador_usuario_documento"))

    # Creacion de la tabla 'ventas'
    op.create_table("ventas",
        # Identificador primario de la venta
        sa.Column("id", sa.Integer, primary_key=True),
        # Identificador UUID de la operacion distribuida
        sa.Column("operacion_id", sa.String(36), nullable=False, unique=True),
        # Usuario que realiza la venta
        sa.Column("usuario", sa.String(100), nullable=False),
        # Clave foranea hacia la tabla de compradores
        sa.Column("comprador_id", sa.Integer,
                  sa.ForeignKey("compradores.id"), nullable=False),
        # Identificador del vehiculo en Java
        sa.Column("vehiculo_id", sa.Integer, nullable=False),
        # Clave de idempotencia provista en la cabecera HTTP
        sa.Column("idempotency_key", sa.String(100), nullable=False),
        # Hash SHA-256 del cuerpo de la solicitud
        sa.Column("request_hash", sa.String(64), nullable=False),
        # Estado de la venta (PENDIENTE, EN_REVISION, COMPLETADA, FALLIDA)
        sa.Column("estado", sa.String(20), nullable=False),
        # Identificador de la reserva en Java
        sa.Column("reserva_id", sa.String(36)),
        # Precio pactado del vehiculo
        sa.Column("precio_acordado", sa.Numeric(12, 2)),
        # Moneda del vehiculo (USD, EUR, etc.)
        sa.Column("moneda", sa.String(3)),
        # Codigo de estado HTTP recibido en caso de fallo remoto
        sa.Column("error_status", sa.Integer),
        # Descripcion del error si la operacion fallo
        sa.Column("error_detalle", sa.String(250)),
        # Estado de la notificacion (PENDIENTE, ENVIADA)
        sa.Column("notificacion", sa.String(20), nullable=False),
        # Fecha y hora de creacion de la venta en UTC
        sa.Column("fecha", sa.DateTime, nullable=False),
        # Restriccion de unicidad para garantizar idempotencia por usuario y clave
        sa.UniqueConstraint("usuario", "idempotency_key",
                            name="uq_venta_usuario_clave"))

# Funcion para revertir los cambios de esta migracion
def downgrade():
    # Se deshabilita el downgrade destructivo por seguridad para evitar perdida accidental de datos
    raise RuntimeError("Reversión con borrado de tablas deshabilitada")
