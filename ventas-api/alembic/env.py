# Modulo de contexto de ejecucion de migraciones de Alembic
from alembic import context

# Importa la clase Base declarativa y el motor de conexion desde app.db
from app.db import Base, engine

# Importa los modelos para que sus metadatos queden registrados en Base.metadata
from app import models

# Conjunto de tablas que pertenecen y son gestionadas exclusivamente por este servicio en Python
TABLAS_PYTHON = {"compradores", "ventas"}

# Metadatos objetivo utilizados por Alembic para autogenerar o comparar esquemas
target_metadata = Base.metadata

# Filtro para ignorar tablas de otros microservicios (por ejemplo tablas creadas por Java o Spring Boot)
def incluir_nombre(nombre, tipo, nombres_padre):
    # Si el elemento analizado es una tabla, solo permite las tablas de este servicio
    if tipo == "table":
        return nombre in TABLAS_PYTHON
    return True

# Filtro avanzado a nivel de objeto para la generacion automatica de migraciones
def incluir_objeto(objeto, nombre, tipo, reflejado, comparar):
    # Asegura que solo se consideren las tablas de ventas y compradores
    if tipo == "table":
        return nombre in TABLAS_PYTHON
    return True

# Funcion para ejecutar migraciones en modo fuera de linea (genera sentencias SQL sin conectarse a la BD)
def run_migrations_offline():
    # Configura el contexto utilizando directamente la URL de la conexion
    context.configure(
        url=str(engine.url),
        target_metadata=target_metadata,
        include_name=incluir_nombre,
        include_object=incluir_objeto,
        compare_type=True,
        literal_binds=True,
    )

    # Inicia una transaccion de migracion y ejecuta los scripts pendientes
    with context.begin_transaction():
        context.run_migrations()

# Funcion para ejecutar migraciones en modo conectado directamente a la base de datos
def run_migrations_online():
    # Conecta directamente al motor de base de datos
    with engine.connect() as connection:
        # Configura el contexto con la conexion activa y los filtros de tablas definidos
        context.configure(
            connection=connection,
            target_metadata=target_metadata,
            include_name=incluir_nombre,
            include_object=incluir_objeto,
            compare_type=True,
        )

        # Ejecuta la migracion dentro de una transaccion de base de datos
        with context.begin_transaction():
            context.run_migrations()

# Determina si Alembic se invoco en modo offline (--sql) o de forma interactiva online
if context.is_offline_mode():
    run_migrations_offline()
else:
    run_migrations_online()
