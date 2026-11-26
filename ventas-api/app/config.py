"""
===============================================================================
ARCHIVO: app/config.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Centraliza la carga, validacion temprana (fail-fast) y exposicion de toda
    la configuracion externa del microservicio desde el archivo `.env` y las
    variables de entorno del sistema operativo. Evita credenciales embebidas en
    codigo fuente y valida la fortaleza minima de las claves inter-servicio.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Configuracion parametrizada por entorno
      (`.env` basado en `.env.example`) con validacion obligatoria al arranque.
    - [Actividad 4]: Expone `JAVA_URL` (via NGINX :8090 o Spring Boot) y
      `SERVICE_KEY` (`X-Service-Key`) para la comunicacion autenticada con Java.
    - [Actividad 8]: Expone `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER`,
      `RABBITMQ_PASSWORD` y `RABBITMQ_QUEUE` (`notificaciones.ventas`) para la
      mensajeria asincrona durable.
    - [Actividad 11]: Impide arrancar en estado inseguro o mal configurado si
      faltan variables criticas o permanecen placeholders `COLOCA_`.
===============================================================================
"""

# [Actividad 1 / Actividad 2] Modulo estandar para leer variables de entorno del proceso
import os

# [Actividad 1 / Actividad 2] Utilidad orientada a objetos para construir rutas absolutas independientes del SO
from pathlib import Path

# [Actividad 1 / Actividad 2] Funcion de python-dotenv que inyecta en `os.environ` las variables definidas en `.env`
from dotenv import load_dotenv

# [Actividad 1 / Actividad 2] Resuelve la raiz del proyecto (`ventas-api/.env`) y carga sus variables sobrescribiendo valores previos
load_dotenv(dotenv_path=Path(__file__).resolve().parent.parent / ".env", override=True)


# [Actividad 1 / Actividad 2] [Actividad 11] Validador fail-fast para variables de entorno indispensables
def requerido(nombre):
    """Obtiene una variable de entorno obligatoria o detiene el arranque si falta o esta vacia."""
    # Lee el valor asociado a `nombre` desde `os.environ`, usando cadena vacia si no existe
    valor = os.environ.get(nombre, "")
    # Verifica que la variable exista y contenga texto no vacio
    if not valor:
        # Falla inmediatamente al iniciar el servicio para evitar errores silenciosos en tiempo de peticion
        raise RuntimeError(f"Falta configurar {nombre}")
    # Devuelve el valor validado de la variable de entorno
    return valor


# [Actividad 1 / Actividad 2] [Actividad 4] URL base de la API Java (normalmente NGINX :8090), sin barra final para concatenar rutas limpiamente
JAVA_URL = requerido("JAVA_URL").rstrip("/")

# [Actividad 4] Clave compartida de servicio enviada en `X-Service-Key` al reservar/confirmar vehiculos en Spring Boot
SERVICE_KEY = requerido("VENTAS_SERVICE_KEY")

# [Actividad 1 / Actividad 2] URL base del servicio de notificaciones (:8081), normalizada sin barra final
NOTIF_URL = requerido("NOTIFICACIONES_URL").rstrip("/")

# [Actividad 1 / Actividad 2] Clave compartida de autenticacion asociada al servicio de notificaciones
NOTIF_KEY = requerido("NOTIFICACIONES_KEY")

# [Actividad 1 / Actividad 2] [Actividad 4] Exige al menos 32 caracteres de entropia en ambas claves inter-servicio
if min(len(SERVICE_KEY), len(NOTIF_KEY)) < 32:
    # Detiene el arranque si alguna clave de servicio es debil o demasiado corta
    raise RuntimeError("Claves de servicio de al menos 32 caracteres")

# [Actividad 1 / Actividad 2] [Actividad 11] Verifica que no se hayan dejado intactos los marcadores de plantilla de `.env.example`
if SERVICE_KEY.startswith("COLOCA_") or NOTIF_KEY.startswith("COLOCA_"):
    # Impide iniciar el microservicio con claves de ejemplo no reemplazadas
    raise RuntimeError("Reemplaza las claves de ejemplo en el archivo .env")

# [Actividad 8] Host del servidor RabbitMQ donde se publicaran eventos de venta completada (por defecto localhost)
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "127.0.0.1")
# [Actividad 8] Puerto AMQP de RabbitMQ convertido a entero (5672 por defecto)
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", "5672"))
# [Actividad 8] Usuario AMQP de RabbitMQ (valor por defecto de desarrollo local si no se define en `.env`)
RABBITMQ_USER = os.environ.get("RABBITMQ_USER", "guest")
# [Actividad 8] Contrasena AMQP de RabbitMQ leida desde entorno (evita quemar credenciales de produccion en codigo)
RABBITMQ_PASSWORD = os.environ.get("RABBITMQ_PASSWORD", "guest")
# [Actividad 8] Nombre de la cola durable en RabbitMQ compartida con el consumidor `notificaciones-api`
RABBITMQ_QUEUE = os.environ.get("RABBITMQ_QUEUE", "notificaciones.ventas")
