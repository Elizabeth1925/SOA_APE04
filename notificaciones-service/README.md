# Microservicio de Notificaciones - Consumidor RabbitMQ y Consulta de Avisos

Microservicio backend desarrollado en Java con Spring Boot, responsable de consumir los eventos asíncronos de venta publicados en RabbitMQ, persistir el historial de notificaciones evitando duplicados por identificador de evento (`eventoId`) y exponer endpoints REST para la consulta de avisos por parte del cliente JavaFX.

---

## 1. Requisitos Previos

* **Kit de desarrollo:** Java JDK 17 o superior (LTS).
* **Gestor de proyectos:** Apache Maven 3.9.0 o superior.
* **Broker de mensajería:** RabbitMQ 3.13+ en ejecución con plugin de administración activo.
* **Puertos de red requeridos:**
  * Puerto de servicio: `8081`.
  * Puerto AMQP de RabbitMQ: `5672`.

---

## 2. Configuración

* **Archivo de configuración:** `src/main/resources/application.properties`.
* **Propiedades principales:**
  * `server.port=8081` (Puerto HTTP del microservicio).
  * `spring.rabbitmq.host=localhost` (Host del servidor RabbitMQ).
  * `spring.rabbitmq.port=5672` (Puerto de conexión AMQP).
  * `spring.rabbitmq.username=guest`
  * `spring.rabbitmq.password=guest`
  * `app.rabbitmq.queue=notificaciones.ventas` (Cola durable de eventos de venta).
  * `servicios.notificaciones.key=avisos-java-python-aula-2026-key-` (Clave de seguridad para peticiones internas).
* **Endpoints expuestos:**
  * `GET /api/notificaciones`: Retorna la lista histórica de notificaciones procesadas para JavaFX.
  * `POST /api/notificaciones`: Canal REST directo para recepción y registro de avisos.

---

## 3. Compilación

Para compilar las fuentes y generar el archivo empaquetado mediante Maven:

```bash
# Navegar al directorio del microservicio
cd notificaciones-service

# Compilar código fuente
mvn clean compile -DskipTests

# Empaquetar en archivo ejecutable .jar
mvn clean package -DskipTests
```

---

## 4. Ejecución

Para iniciar el microservicio desde la consola:

```bash
# Iniciar mediante Spring Boot Maven Plugin
mvn spring-boot:run
```

O ejecutando el paquete binario generado:

```bash
java -jar target/notificaciones-service-0.0.1-SNAPSHOT.jar
```

El servicio iniciará el listener de RabbitMQ y quedará escuchando en `http://localhost:8081`.
