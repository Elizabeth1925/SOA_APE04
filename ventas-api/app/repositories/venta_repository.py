# Operadores de consulta SQL de SQLAlchemy
from sqlalchemy import select, or_

# Modelo ORM de la entidad Venta
from app.models import Venta

# Repositorio que encapsula las operaciones de acceso a datos para la entidad Venta
class VentaRepository:
    # Recibe la sesion activa de SQLAlchemy para ejecutar consultas
    def __init__(self, db):
        self.db = db

    # Busca una venta existente para un usuario dado segun su clave de idempotencia
    def por_clave(self, usuario, clave):
        return self.db.scalar(select(Venta).where(
            Venta.usuario == usuario, Venta.idempotency_key == clave))

    # Obtiene una venta bloqueando la fila a nivel de base de datos (SELECT ... FOR UPDATE) para concurrencia segura
    def bloquear(self, id):
        return self.db.scalar(select(Venta).where(Venta.id == id)
                              .with_for_update())

    # Obtiene una lista de identificadores de ventas que requieren procesamiento o reintento de notificaciones
    def pendientes(self):
        # Selecciona los primeros 100 IDs cuyo estado sea PENDIENTE, EN_REVISION o COMPLETADA con notificacion pendiente
        return self.db.scalars(select(Venta.id).where(or_(
            Venta.estado.in_(["PENDIENTE", "EN_REVISION"]),
            (Venta.estado == "COMPLETADA") &
            (Venta.notificacion != "ENVIADA")))
            .order_by(Venta.id).limit(100)).all()
