# Modulo del sistema operativo para acceder a variables de entorno opcionales
import os

# Clases de SQLAlchemy para construir la conexion (engine) y estructurar URLs de conexion de base de datos
from sqlalchemy import create_engine, URL
# Clases de SQLAlchemy ORM para declarar la clase base de modelos y crear fabricas de sesiones
from sqlalchemy.orm import DeclarativeBase, sessionmaker

# Importa la funcion 'requerido' definida en la configuracion para validar variables obligatorias
from app.config import requerido

# Construccion segura del objeto URL de conexion para SQLAlchemy
url = URL.create(
    # Especifica el dialecto y el driver de conexion: MySQL usando PyMySQL
    "mysql+pymysql",
    # Usuario de la base de datos obtenido de las variables de entorno
    username=requerido("DB_USER"),
    # Contrasena de la base de datos (por defecto cadena vacia si no existe)
    password=os.getenv("DB_PASSWORD", ""),
    # Host o direccion IP del servidor de base de datos
    host=requerido("DB_HOST"),
    # Puerto de conexion a MySQL convertido a entero (3306 por defecto)
    port=int(os.getenv("DB_PORT", "3306")),
    # Nombre de la base de datos relacional a utilizar
    database=requerido("DB_NAME"),
    # Parametros adicionales de consulta: codificacion UTF-8 completa (soporta emojis y caracteres especiales)
    query={"charset": "utf8mb4"},
)

# Creacion del motor de base de datos (Engine) de SQLAlchemy
# pool_pre_ping=True verifica la salud de la conexion antes de usarla evitando conexiones caidas
# isolation_level="READ COMMITTED" evita lecturas sucias asegurando transacciones consistentes
engine = create_engine(url, pool_pre_ping=True,
                       isolation_level="READ COMMITTED")

# Fabrica de sesiones locales de base de datos
# expire_on_commit=False evita que los atributos del objeto expiren despues de hacer commit, permitiendo usarlos fuera de la transaccion
SessionLocal = sessionmaker(engine, expire_on_commit=False)

# Clase base declarativa de la cual heredaran todos los modelos ORM de la aplicacion
class Base(DeclarativeBase):
    pass
