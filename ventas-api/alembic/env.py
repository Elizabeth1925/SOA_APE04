"""
===============================================================================
ARCHIVO: alembic/env.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Entorno de ejecucion de migraciones de esquema gestionadas por Alembic.
    Conecta el motor SQLAlchemy de `ventas-api` con los metadatos declarativos
    de sus modelos ORM y aisla estrictamente la propiedad de tablas dentro de
    la base de datos MySQL compartida (`venta_vehiculos`), de modo que Alembic
    unicamente administre `compradores` y `ventas` sin interferir con las tablas
    administradas por Flyway/JPA en el microservicio Spring Boot (Java).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Infraestructura de migraciones versionadas
      con Alembic en modo offline (generacion SQL) y online (aplicacion DDL),
      protegiendo el aislamiento de esquemas entre microservicios SOA.
    - [Actividad 4]: Registro de metadatos de las entidades nucleo `Comprador`
      y `Venta` (incluyendo restricciones de unicidad e idempotencia).
===============================================================================
"""

# [Actividad 1 / Actividad 2] Importa el contexto de ejecucion de Alembic que orquesta migraciones online y offline
from alembic import context

# [Actividad 1 / Actividad 2] Importa la clase declarativa Base y el motor SQLAlchemy configurado con variables de `.env`
from app.db import Base, engine

# [Actividad 1 / Actividad 2] [Actividad 4] Importa el modulo de modelos ORM para poblar `Base.metadata` antes de inspeccionar tablas
from app import models

# [Actividad 1 / Actividad 2] Lista blanca de tablas cuyo ciclo de vida DDL pertenece exclusivamente al microservicio Python `ventas-api`
TABLAS_PYTHON = {"compradores", "ventas"}

# [Actividad 1 / Actividad 2] Metadatos ORM de referencia usados por Alembic para comparar y autogenerar revisiones de esquema
target_metadata = Base.metadata


# [Actividad 1 / Actividad 2] Filtro por nombre para evitar que Alembic altere o elimine tablas pertenecientes a Spring Boot (Java)
def incluir_nombre(nombre, tipo, nombres_padre):
    """Permite procesar solo las tablas propiedad de `ventas-api` durante la reflexion de esquema."""
    # Verifica si el objeto inspeccionado en la base de datos corresponde a una tabla
    if tipo == "table":
        # Retorna True unicamente si la tabla esta declarada dentro de `TABLAS_PYTHON`
        return nombre in TABLAS_PYTHON
    # Permite otros elementos dependientes (indices, columnas, restricciones) de las tablas aceptadas
    return True


# [Actividad 1 / Actividad 2] Filtro de objetos ORM/reflejados aplicado durante la comparacion y autogeneracion de migraciones
def incluir_objeto(objeto, nombre, tipo, reflejado, comparar):
    """Restringe la inspeccion de objetos de Alembic exclusivamente a las tablas `compradores` y `ventas`."""
    # Comprueba si el objeto evaluado por Alembic es de tipo tabla
    if tipo == "table":
        # Acepta solo tablas de este microservicio para mantener fronteras claras en la BD compartida
        return nombre in TABLAS_PYTHON
    # Mantiene visibles los demas elementos de esquema vinculados a las tablas permitidas
    return True


# [Actividad 1 / Actividad 2] Ejecuta migraciones en modo offline (emite sentencias SQL sin abrir sesion activa contra MySQL)
def run_migrations_offline():
    """Configura y ejecuta Alembic en modo offline generando DDL con valores literales."""
    # Configura el contexto offline tomando directamente la URL construida en `app/db.py`
    context.configure(
        # Serializa la URL de conexion SQLAlchemy para que Alembic identifique el dialecto MySQL
        url=str(engine.url),
        # Asocia los metadatos declarativos de los modelos `Comprador` y `Venta`
        target_metadata=target_metadata,
        # Aplica el filtro de nombres para excluir tablas de otros microservicios SOA
        include_name=incluir_nombre,
        # Aplica el filtro de objetos para aislar las tablas gestionadas por Python
        include_object=incluir_objeto,
        # Habilita la deteccion de cambios en tipos de columna (por ejemplo longitudes de VARCHAR o Numeric)
        compare_type=True,
        # Renderiza parametros como literales directamente en la salida SQL offline
        literal_binds=True,
    )

    # Abre el bloque transaccional logico del script offline
    with context.begin_transaction():
        # Ejecuta secuencialmente todas las migraciones pendientes hacia la revision objetivo
        context.run_migrations()


# [Actividad 1 / Actividad 2] Ejecuta migraciones en modo online aplicando cambios directamente sobre MySQL
def run_migrations_online():
    """Abre una conexion al motor MySQL y aplica las migraciones pendientes dentro de una transaccion."""
    # Solicita una conexion fisica al pool del motor SQLAlchemy (`engine`)
    with engine.connect() as connection:
        # Configura el contexto de Alembic sobre la conexion activa hacia MySQL
        context.configure(
            # Inyecta la conexion activa sobre la que se ejecutaran las sentencias DDL
            connection=connection,
            # Registra los metadatos declarativos de `app.models` como estado esperado
            target_metadata=target_metadata,
            # Restringe la reflexion por nombre a las tablas de `ventas-api`
            include_name=incluir_nombre,
            # Restringe el arbol de objetos inspeccionados a las tablas de `ventas-api`
            include_object=incluir_objeto,
            # Activa comparacion estricta de tipos de datos al detectar diferencias de esquema
            compare_type=True,
        )

        # Inicia una transaccion en la base de datos para aplicar la migracion de manera atomica
        with context.begin_transaction():
            # Ejecuta los metodos `upgrade()` o `downgrade()` de las revisiones pendientes
            context.run_migrations()


# [Actividad 1 / Actividad 2] Determina el modo de ejecucion invocado desde la CLI de Alembic (`--sql` vs conexion directa)
if context.is_offline_mode():
    # Invoca el flujo offline cuando se solicito emision de SQL sin conexion
    run_migrations_offline()
else:
    # Invoca el flujo online por defecto para migrar la base de datos MySQL activa
    run_migrations_online()
