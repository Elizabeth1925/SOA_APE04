"""
===============================================================================
ARCHIVO: app/db.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Configura la capa de acceso a datos relacional con SQLAlchemy 2.0 sobre
    MySQL (`pymysql`). Construye de forma segura la URL de conexion a partir de
    variables de entorno, define el pool de conexiones con verificacion de salud
    (`pool_pre_ping`), nivel de aislamiento `READ COMMITTED`, fabrica de
    sesiones `SessionLocal` y la clase declarativa `Base` para los modelos ORM.

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Conexion parametrizada a MySQL via `app.config`
      y proveedor de `engine`/`Base` para las migraciones Alembic (`alembic/env.py`).
    - [Actividad 4]: Soporta el patron de aislamiento transaccional en 3 fases
      mediante `SessionLocal` con `expire_on_commit=False` y aislamiento
      `READ COMMITTED`, evitando lecturas sucias y sesiones colgadas.
    - [Actividad 11]: `pool_pre_ping=True` detecta conexiones caidas ante fallos
      de MySQL para propagar errores reales capturados como `503 BD_NO_DISPONIBLE`.
===============================================================================
"""

# [Actividad 1 / Actividad 2] Modulo del sistema operativo para leer variables opcionales con valores por defecto
import os

# [Actividad 1 / Actividad 2] Constructores de SQLAlchemy para instanciar el motor de BD y armar URLs escapadas de forma segura
from sqlalchemy import create_engine, URL
# [Actividad 1 / Actividad 2] [Actividad 4] Clases ORM de SQLAlchemy 2.0 para la base declarativa y la fabrica de sesiones
from sqlalchemy.orm import DeclarativeBase, sessionmaker

# [Actividad 1 / Actividad 2] Importa el validador de variables obligatorias para exigir host, usuario y nombre de BD
from app.config import requerido

# [Actividad 1 / Actividad 2] Construye el objeto URL escapando caracteres especiales en usuario/contrasena sin interpolar strings manuales
url = URL.create(
    # Dialecto SQLAlchemy `mysql` con driver puro Python `pymysql`
    "mysql+pymysql",
    # Usuario de base de datos exigido en `.env`
    username=requerido("DB_USER"),
    # Contrasena de base de datos opcional (cadena vacia si el entorno local no requiere clave)
    password=os.getenv("DB_PASSWORD", ""),
    # Direccion IP o dominio del servidor MySQL exigido en `.env`
    host=requerido("DB_HOST"),
    # Puerto TCP de MySQL convertido a entero (3306 estandar por defecto)
    port=int(os.getenv("DB_PORT", "3306")),
    # Nombre de la base de datos relacional (`venta_vehiculos`) exigido en `.env`
    database=requerido("DB_NAME"),
    # Parametro de conexion para habilitar UTF-8 completo de 4 bytes (`utf8mb4`, soporta acentos y caracteres internacionales)
    query={"charset": "utf8mb4"},
)

# [Actividad 4] [Actividad 11] Instancia el motor de conexiones SQLAlchemy con verificacion previa y aislamiento consistente
# - `pool_pre_ping=True`: valida la conexion antes de prestarla del pool y detecta reinicios/caidas de MySQL (soporte P13)
# - `isolation_level="READ COMMITTED"`: garantiza lectura de datos confirmados y coopera con bloqueos `SELECT ... FOR UPDATE`
engine = create_engine(url, pool_pre_ping=True,
                       isolation_level="READ COMMITTED")

# [Actividad 4] Fabrica de sesiones por peticion o bloque `with SessionLocal()` / `with SessionLocal.begin()`
# `expire_on_commit=False` permite leer atributos de entidades ya confirmadas despues de cerrar la transaccion corta
SessionLocal = sessionmaker(engine, expire_on_commit=False)


# [Actividad 1 / Actividad 2] [Actividad 4] Clase base declarativa que registra metadatos de `Comprador` y `Venta`
class Base(DeclarativeBase):
    """Raiz declarativa del mapeo ORM de SQLAlchemy 2.0 para `ventas-api`."""
    # No requiere atributos adicionales; SQLAlchemy gestiona `metadata` automaticamente
    pass
