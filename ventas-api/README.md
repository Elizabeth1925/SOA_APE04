# Microservicio de Ventas y Compradores - Python FastAPI

Microservicio backend desarrollado en Python con FastAPI y SQLAlchemy, responsable de la gestión de compradores, el procesamiento transaccional de ventas con estricta garantía de idempotencia (cabecera `Idempotency-Key` y hash SHA-256), la persistencia en MySQL y la publicación asíncrona de eventos de venta en RabbitMQ una vez confirmada la operación.

---

## 1. Requisitos Previos

* **Lenguaje:** Python v3.12 o superior.
* **Gestor de paquetes:** pip y herramienta de entornos virtuales `venv`.
* **Servidor MySQL:** XAMPP activo en `localhost:3306` con la base de datos `venta_vehiculos`.
* **Broker de mensajería:** RabbitMQ 3.13+ activo en puerto AMQP `5672`.
* **Gateway NGINX:** Activo en puerto `8090` para comunicación inter-servicios.
* **Microservicio Java:** API de Vehículos activa en puerto `8080` para coordinar reservas.

---

## 2. Configuración

* **Archivo de entorno:** Crear el archivo `.env` en la raíz de `ventas-api` basándose en `.env.example`:
  ```bash
  # Copiar la plantilla sin credenciales sensibles
  copy .env.example .env
  ```
* **Variables esenciales en `.env`:**
  * `DB_HOST=127.0.0.1`
  * `DB_PORT=3306`
  * `DB_NAME=venta_vehiculos`
  * `DB_USER=root`
  * `DB_PASSWORD=` (vacío para XAMPP)
  * `JAVA_URL=http://127.0.0.1:8080` (o `http://127.0.0.1:8090` vía Gateway)
  * `VENTAS_SERVICE_KEY=ventas-java-python-aula-2026-key-`
  * `RABBITMQ_HOST=127.0.0.1`
  * `RABBITMQ_PORT=5672`
  * `RABBITMQ_QUEUE=notificaciones.ventas`
* **Endpoints expuestos:**
  * `POST /api/compradores`: Registra un comprador asociado al usuario autenticado.
  * `GET /api/compradores`: Lista compradores autorizados.
  * `POST /api/ventas`: Registra una venta idempotente requiriendo `Idempotency-Key`.
  * `GET /api/ventas`: Consulta listado general de ventas para administración y React.
  * `GET /api/ventas/{id}`: Consulta el estado detallado de una venta individual.

---

## 3. Instalación y Migraciones (Compilación)

En el ecosistema Python, la preparación del entorno y la ejecución de migraciones de base de datos constituyen el proceso equivalente a la compilación:

```bash
# Navegar al directorio del microservicio
cd ventas-api

# Crear entorno virtual de Python
python -m venv .venv

# Activar el entorno virtual
# En Windows PowerShell:
.\.venv\Scripts\Activate.ps1
# O en CMD:
.\.venv\Scripts\activate.bat

# Instalar dependencias requeridas
pip install -r requirements.txt

# Aplicar migraciones del esquema con Alembic
alembic upgrade head
```

---

## 4. Ejecución

Para iniciar el servidor ASGI Uvicorn en el puerto 8002:

```bash
# Iniciar Uvicorn con recarga en vivo para desarrollo
uvicorn app.main:app --host 127.0.0.1 --port 8002 --reload
```

* **Documentación interactiva Swagger UI:** [http://127.0.0.1:8002/docs](http://127.0.0.1:8002/docs).
* **Documentación ReDoc:** [http://127.0.0.1:8002/redoc](http://127.0.0.1:8002/redoc).
