# Modulo para acceder a parametros de linea de comandos pasados al script
import sys

# Modulo para pausas de ejecucion entre ciclos (sleep)
import time

# Modulo de logging para registrar fallos o eventos durante la ejecucion en segundo plano
import logging

# Fabrica de sesiones de base de datos
from app.db import SessionLocal

# Repositorio para consultar ventas pendientes de conciliacion o notificacion
from app.repositories.venta_repository import VentaRepository

# Funciones de procesamiento de transacciones remotas y envio de notificaciones
from app.services.venta_service import procesar, notificar

# Funcion que ejecuta una iteracion del ciclo de recuperacion y conciliacion
def ciclo():
    # Consulta la lista de identificadores de ventas que quedaron pendientes o en revision
    with SessionLocal() as db:
        ids = VentaRepository(db).pendientes()

    # Itera sobre cada venta pendiente
    for id in ids:
        try:
            # Reintenta el procesamiento remoto con el servicio de Java (reserva / confirmacion)
            procesar(id)
            # Reintenta el envio de la notificacion si la venta ya fue completada
            notificar(id)
        except Exception:
            # Captura y registra cualquier excepcion ocurrida durante la conciliacion de esa venta en particular
            logging.exception("No se pudo conciliar venta %s", id)

# Punto de entrada cuando el worker se ejecuta como script independiente
if __name__ == "__main__":
    # Bucle infinito para ejecucion continua como servicio o tarea en segundo plano
    while True:
        try:
            # Ejecuta un ciclo de conciliacion
            ciclo()
        except Exception:
            # Registra errores criticos que puedan ocurrir a nivel del ciclo
            logging.exception("Fallo de ciclo de recuperación")

        # Si se paso el argumento '--once', se ejecuta una unica vez y finaliza (util para pruebas o cron jobs)
        if "--once" in sys.argv:
            break

        # Espera 10 segundos antes de comenzar la siguiente iteracion
        time.sleep(10)
