"""
===============================================================================
ARCHIVO: app/repositories/venta_repository.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Encapsula las consultas criticas de acceso a datos de la entidad `Venta`
    en MySQL mediante SQLAlchemy 2.0: busqueda por clave de idempotencia,
    bloqueo pesimista a nivel de fila (`SELECT ... FOR UPDATE`) para la Fase 3
    de actualizaciones atomicas y seleccion en lotes de registros pendientes
    para el worker de conciliacion/Outbox.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 4]: Busqueda `por_clave()` para deduplicacion por
      `(usuario, idempotency_key)` y `bloquear()` con `FOR UPDATE` para
      actualizar de forma segura bajo concurrencia al cerrar la Fase 3 del
      aislamiento transaccional.
    - [Actividad 8]: Consulta `pendientes()` del patron Outbox simplificado,
      utilizada por `app/worker.py` para recuperar ventas `COMPLETADA` cuya
      notificacion RabbitMQ aun permanece en estado distinto de `ENVIADA`.
    - [Actividad 11]: Tambien selecciona ventas `PENDIENTE` y `EN_REVISION`
      para conciliar fallos transitorios de comunicacion con Spring Boot (P13).
===============================================================================
"""

# [Actividad 4] [Actividad 8] Constructores de consulta SQLAlchemy 2.0 (`select` y disyuncion logica `or_`)
from sqlalchemy import select, or_

# [Actividad 4] Modelo ORM `Venta` mapeado a la tabla MySQL `ventas`
from app.models import Venta


# [Actividad 4] [Actividad 8] Repositorio especializado para consultas y bloqueos sobre la tabla `ventas`
class VentaRepository:
    """Agrupa las operaciones SQL reutilizables de idempotencia, bloqueo de fila y cola Outbox."""

    # Recibe una sesion SQLAlchemy activa inyectada desde el servicio o el worker
    def __init__(self, db):
        """Asocia el repositorio con la sesion de base de datos suministrada."""
        # Almacena la referencia de sesion para ejecutar las consultas de instancia
        self.db = db

    # [Actividad 4] Busca una venta previa usando exactamente los campos de la restriccion unica `uq_venta_usuario_clave`
    def por_clave(self, usuario, clave):
        """Retorna la `Venta` existente para `(usuario, idempotency_key)` o `None` si es nueva (P11)."""
        # Ejecuta `SELECT * FROM ventas WHERE usuario = ? AND idempotency_key = ?` devolviendo escalar unico o None
        return self.db.scalar(select(Venta).where(
            Venta.usuario == usuario, Venta.idempotency_key == clave))

    # [Actividad 4] [Actividad 8] Obtiene la fila de venta con bloqueo pesimista `SELECT ... FOR UPDATE` en Fase 3
    def bloquear(self, id):
        """Bloquea exclusivamente la fila `ventas.id` dentro de una transaccion corta para evitar condiciones de carrera."""
        # Garantiza serializacion segura al escribir el estado final de negocio o la bandera `notificacion = "ENVIADA"`
        return self.db.scalar(select(Venta).where(Venta.id == id)
                              .with_for_update())

    # [Actividad 8] [Actividad 11] Selecciona el lote de ventas que requieren conciliacion remota o reintento RabbitMQ
    def pendientes(self):
        """Retorna hasta 100 IDs ordenados ascendentemente para el patron Outbox y recuperacion en `worker.py`."""
        # Selecciona solo la columna `Venta.id` filtrando dos casos:
        # 1) Ventas inconclusas (`PENDIENTE` o `EN_REVISION`) que deben conciliarse con Spring Boot
        # 2) Ventas `COMPLETADA` cuya notificacion RabbitMQ aun no fue `ENVIADA` (Outbox tras caida de RabbitMQ, P13)
        return self.db.scalars(select(Venta.id).where(or_(
            Venta.estado.in_(["PENDIENTE", "EN_REVISION"]),
            (Venta.estado == "COMPLETADA") &
            (Venta.notificacion != "ENVIADA")))
            .order_by(Venta.id).limit(100)).all()
