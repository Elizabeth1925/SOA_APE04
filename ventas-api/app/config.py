# Modulo estandar del sistema operativo para leer variables de entorno y rutas
import os

# Modulo para manipular rutas del sistema de archivos de forma orientada a objetos
from pathlib import Path

# Carga variables de entorno desde un archivo .env en el entorno de ejecucion
from dotenv import load_dotenv

# Localiza la ruta raiz del proyecto (dos niveles arriba de este archivo) y carga el archivo .env
load_dotenv(dotenv_path=Path(__file__).resolve().parent.parent / ".env", override=True)

# Funcion auxiliar para obtener una variable de entorno obligatoria; lanza error si no existe o esta vacia
def requerido(nombre):
    # Obtiene el valor de la variable de entorno solicitada, usando cadena vacia por defecto
    valor = os.environ.get(nombre, "")
    # Valida si el valor no esta definido o se encuentra vacio
    if not valor:
        # Lanza una excepcion en tiempo de ejecucion indicando la variable faltante
        raise RuntimeError(f"Falta configurar {nombre}")
    # Retorna el valor obtenido de la variable de entorno
    return valor

# URL base del microservicio en Java, eliminando cualquier barra final ('/') para consistencia en las rutas
JAVA_URL = requerido("JAVA_URL").rstrip("/")

# Clave de seguridad requerida para autorizar llamadas desde este microservicio hacia Java
SERVICE_KEY = requerido("VENTAS_SERVICE_KEY")

# URL base del microservicio de notificaciones, eliminando la barra final ('/') para evitar URLs duplicadas
NOTIF_URL = requerido("NOTIFICACIONES_URL").rstrip("/")

# Clave de seguridad para autenticarse ante el microservicio de notificaciones
NOTIF_KEY = requerido("NOTIFICACIONES_KEY")

# Verifica que ambas claves de seguridad cumplan con una longitud minima de 32 caracteres por estandares de seguridad
if min(len(SERVICE_KEY), len(NOTIF_KEY)) < 32:
    raise RuntimeError("Claves de servicio de al menos 32 caracteres")

# Valida que el desarrollador no haya dejado los valores por defecto del archivo de ejemplo (.env.example)
if SERVICE_KEY.startswith("COLOCA_") or NOTIF_KEY.startswith("COLOCA_"):
    raise RuntimeError("Reemplaza las claves de ejemplo en el archivo .env")

# Configuracion de RabbitMQ para publicacion de eventos de venta (APE 04)
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "127.0.0.1")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.environ.get("RABBITMQ_USER", "guest")
RABBITMQ_PASSWORD = os.environ.get("RABBITMQ_PASSWORD", "guest")
RABBITMQ_QUEUE = os.environ.get("RABBITMQ_QUEUE", "notificaciones.ventas")
