"""
===============================================================================
ARCHIVO: app/worker.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Worker autonomo en segundo plano que implementa el patron Outbox
    simplificado y de conciliacion eventual para `ventas-api`. Periodicamente
    consulta las ventas en MySQL que quedaron pendientes de conciliacion con
    Java (`PENDIENTE` / `EN_REVISION`) o pendientes de publicacion de evento en
    RabbitMQ (`estado == COMPLETADA` y `notificacion != ENVIADA`) y reintenta
    su resolucion de manera idempotente.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 8]: Patron Outbox simplificado junto a `VentaRepository.pendientes()`
      y `venta_service.notificar()`: si RabbitMQ estuvo caido durante el registro
      de una venta, este worker reintenta automaticamente la publicacion durable
      cuando el broker se recupera, hasta marcar `notificacion = "ENVIADA"`.
    - [Actividad 4]: Concilia ventas que quedaron en estado `PENDIENTE` o
      `EN_REVISION` (HTTP 202) ejecutando `procesar(id)` con aislamiento
      transaccional fuera de MySQL.
    - [Actividad 11]: Tolera fallos reales transitorios de Java, MySQL o
      RabbitMQ sin detener el proceso ni generar confirmaciones falsas (P13).
===============================================================================
"""

# [Actividad 8] Modulo del sistema usado para detectar la bandera CLI `--once` (ejecucion unica en pruebas/cron)
import sys

# [Actividad 8] Modulo estandar usado para pausar el bucle de polling entre ciclos de recuperacion
import time

# [Actividad 8] [Actividad 11] Modulo de logging para registrar trazas de errores sin tumbar el worker
import logging

# [Actividad 4] [Actividad 8] Fabrica de sesiones SQLAlchemy para abrir consultas cortas en cada ciclo
from app.db import SessionLocal

# [Actividad 8] Repositorio que selecciona los IDs de ventas pendientes de conciliacion o notificacion Outbox
from app.repositories.venta_repository import VentaRepository

# [Actividad 4] [Actividad 8] Servicios idempotentes de conciliacion remota con Java (`procesar`) y emision RabbitMQ (`notificar`)
from app.services.venta_service import procesar, notificar


# [Actividad 8] [Actividad 11] Ejecuta un barrido completo de recuperacion Outbox y conciliacion distribuida
def ciclo():
    """Consulta ventas pendientes en MySQL y reintenta procesarlas y notificarlas una por una."""
    # [Actividad 8] Abre una sesion corta de solo lectura para obtener hasta 100 IDs pendientes (sin retener la conexion)
    with SessionLocal() as db:
        # Consulta mediante `VentaRepository.pendientes()` ventas `PENDIENTE`, `EN_REVISION` o con `notificacion != ENVIADA`
        ids = VentaRepository(db).pendientes()

    # [Actividad 8] Recorre secuencialmente cada identificador de venta pendiente para aislar fallos individuales
    for id in ids:
        # Encapsula cada venta en su propio `try/except` para que un fallo puntual no bloquee el resto del lote
        try:
            # [Actividad 4] [Actividad 11] Si la venta esta `PENDIENTE` o `EN_REVISION`, concilia su reserva/confirmacion en Java
            procesar(id)
            # [Actividad 8] Si la venta ya esta `COMPLETADA` y `notificacion != ENVIADA`, reintenta publicar en RabbitMQ
            notificar(id)
        # [Actividad 11] Registra cualquier error real ocurrido al conciliar esta venta y continua con el siguiente ID
        except Exception:
            # Loguea la excepcion completa con el identificador de la venta afectada
            logging.exception("No se pudo conciliar venta %s", id)


# [Actividad 8] Punto de entrada CLI del worker (`python -m app.worker` o `python -m app.worker --once`)
if __name__ == "__main__":
    # Bucle continuo del proceso demonio de recuperacion Outbox
    while True:
        # Protege el ciclo completo frente a fallos globales temporales (por ejemplo caida momentanea de MySQL)
        try:
            # Ejecuta una pasada de conciliacion y reenvio de notificaciones pendientes
            ciclo()
        # [Actividad 11] Captura errores criticos de ciclo (como BD no disponible temporalmente) sin abortar el demonio
        except Exception:
            # Registra la traza del fallo de ciclo para observabilidad operativa
            logging.exception("Fallo de ciclo de recuperación")

        # [Actividad 8] Permite ejecucion determinista de una sola iteracion en pruebas automatizadas o tareas programadas
        if "--once" in sys.argv:
            # Sale limpiamente del bucle tras completar la unica pasada
            break

        # [Actividad 8] Espera 10 segundos antes del siguiente sondeo para no saturar MySQL, Java ni RabbitMQ
        time.sleep(10)
