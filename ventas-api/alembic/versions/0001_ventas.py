"""
===============================================================================
ARCHIVO: alembic/versions/0001_ventas.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Migracion inicial de esquema relacional en MySQL para el dominio de
    Compradores y Ventas. Define la estructura DDL autoritativa de las tablas
    `compradores` y `ventas`, sus claves foraneas, columnas de trazabilidad
    distribuida, soporte de idempotencia y estado de notificacion asincrona.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Primera migracion versionada de Alembic
      (`revision = "0001"`) con bloqueo de `downgrade()` destructivo para
      proteger los datos historicos del sistema.
    - [Actividad 4]: Crea la restriccion unica `uq_comprador_usuario_documento`
      y la restriccion unica de idempotencia `uq_venta_usuario_clave`, ademas
      de columnas de precio acordado, moneda, huella SHA-256 (`request_hash`)
      y estado de la transaccion de venta.
    - [Actividad 8]: Incluye la columna `notificacion` (`PENDIENTE` / `ENVIADA`)
      que respalda el patron Outbox simplificado junto con `worker.py`.
    - [Actividad 10 - P27]: Garantiza a nivel de BD la coherencia referencial y
      trazabilidad de `comprador_id`, `vehiculo_id`, `id` (`ventaId`) y
      `operacion_id` (`unique=True`).
    - [Actividad 11]: Define `error_status` y `error_detalle` para registrar
      fallos reales de comunicacion o rechazo sin fabricar confirmaciones.
===============================================================================
"""

# [Actividad 1 / Actividad 2] Importa las operaciones DDL de Alembic (`create_table`, etc.)
from alembic import op

# [Actividad 1 / Actividad 2] Importa el modulo principal de SQLAlchemy para definir columnas, tipos y restricciones
import sqlalchemy as sa

# [Actividad 1 / Actividad 2] Identificador unico de esta migracion dentro del historial de versiones de Alembic
revision = "0001"

# [Actividad 1 / Actividad 2] Revision predecesora; `None` indica que esta es la migracion raiz inicial
down_revision = None

# [Actividad 1 / Actividad 2] Etiquetas de rama opcionales para arboles de migracion complejos (no usadas aqui)
branch_labels = None

# [Actividad 1 / Actividad 2] Dependencias respecto a otras ramas de migracion (no aplica al ser independiente)
depends_on = None


# [Actividad 1 / Actividad 2] [Actividad 4] Funcion ejecutada por Alembic al aplicar `alembic upgrade head`
def upgrade():
    """Crea las tablas `compradores` y `ventas` con sus restricciones de integridad e idempotencia."""
    # [Actividad 4] Crea la tabla `compradores` para almacenar clientes registrados por vendedores/operadores
    op.create_table("compradores",
        # [Actividad 10 - P27] Clave primaria autoincremental del comprador (`compradorId`)
        sa.Column("id", sa.Integer, primary_key=True),
        # [Actividad 4] Nombre de usuario propietario/vendedor autenticado via Java que registro al comprador
        sa.Column("usuario", sa.String(100), nullable=False),
        # [Actividad 4] Documento de identidad (cedula, pasaporte o RUC) obligatorio para el comprador
        sa.Column("documento", sa.String(30), nullable=False),
        # [Actividad 4] Nombre completo o razon social del comprador
        sa.Column("nombre", sa.String(120), nullable=False),
        # [Actividad 4] Correo electronico del comprador (almacenado en BD; no se propaga como PII en eventos RabbitMQ)
        sa.Column("correo", sa.String(200), nullable=False),
        # [Actividad 4] [Actividad 11] Restriccion unica por usuario y documento para impedir duplicados (HTTP 409)
        sa.UniqueConstraint("usuario", "documento",
                            name="uq_comprador_usuario_documento"))

    # [Actividad 4] [Actividad 8] [Actividad 10 - P27] Crea la tabla `ventas` con soporte de idempotencia y Outbox
    op.create_table("ventas",
        # [Actividad 10 - P27] Clave primaria autoincremental de la venta (`ventaId`)
        sa.Column("id", sa.Integer, primary_key=True),
        # [Actividad 10 - P27] UUID v4 unico que correlaciona la venta entre `ventas-api`, `vehiculos-api` y RabbitMQ (`operacionId`)
        sa.Column("operacion_id", sa.String(36), nullable=False, unique=True),
        # [Actividad 4] Usuario autenticado responsable de iniciar la venta (usado en RBAC y aislamiento por propietario)
        sa.Column("usuario", sa.String(100), nullable=False),
        # [Actividad 10 - P27] Clave foranea obligatoria hacia `compradores.id` garantizando integridad referencial de `compradorId`
        sa.Column("comprador_id", sa.Integer,
                  sa.ForeignKey("compradores.id"), nullable=False),
        # [Actividad 10 - P27] Identificador numerico del vehiculo gestionado en Spring Boot (`vehiculoId`)
        sa.Column("vehiculo_id", sa.Integer, nullable=False),
        # [Actividad 4] Clave recibida en la cabecera `Idempotency-Key` para deduplicar reintentos del cliente
        sa.Column("idempotency_key", sa.String(100), nullable=False),
        # [Actividad 4] [Actividad 11] Huella SHA-256 hexadecimal (64 chars) del cuerpo JSON canonico para detectar reutilizacion maliciosa/distinta (409)
        sa.Column("request_hash", sa.String(64), nullable=False),
        # [Actividad 4] [Actividad 11] Maquina de estados de la venta: `PENDIENTE`, `EN_REVISION`, `COMPLETADA` o `FALLIDA`
        sa.Column("estado", sa.String(20), nullable=False),
        # [Actividad 4] Identificador UUID de la reserva devuelto por Spring Boot al bloquear el vehiculo
        sa.Column("reserva_id", sa.String(36)),
        # [Actividad 4] [Actividad 11] Precio final acordado (12 digitos, 2 decimales) tomado estrictamente desde Java (integridad de precios)
        sa.Column("precio_acordado", sa.Numeric(12, 2)),
        # [Actividad 4] Codigo ISO de moneda (3 caracteres, ej. USD) provisto por Spring Boot en la reserva
        sa.Column("moneda", sa.String(3)),
        # [Actividad 11] Codigo HTTP de rechazo/error remoto (404, 409, 422, etc.) persistido cuando una venta falla
        sa.Column("error_status", sa.Integer),
        # [Actividad 11] Mensaje diagnostico del fallo real o estado de revision pendiente sin simular exito
        sa.Column("error_detalle", sa.String(250)),
        # [Actividad 8] Bandera del patron Outbox simplificado (`PENDIENTE` o `ENVIADA`) para garantizar publicacion unica en RabbitMQ
        sa.Column("notificacion", sa.String(20), nullable=False),
        # [Actividad 4] [Actividad 10 - P27] Marca temporal UTC de creacion de la venta
        sa.Column("fecha", sa.DateTime, nullable=False),
        # [Actividad 4] [Actividad 11] Restriccion unica atomica en BD que protege la idempotencia incluso ante solicitudes concurrentes
        sa.UniqueConstraint("usuario", "idempotency_key",
                            name="uq_venta_usuario_clave"))


# [Actividad 1 / Actividad 2] Funcion de reversion de migracion protegida contra eliminacion accidental de datos
def downgrade():
    """Bloquea explicitamente el borrado destructivo de las tablas `compradores` y `ventas`."""
    # Lanza un error en tiempo de ejecucion para impedir perdida involuntaria de registros financieros en produccion
    raise RuntimeError("Reversión con borrado de tablas deshabilitada")
