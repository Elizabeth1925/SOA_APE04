# Microservicio de Vehículos y Autenticación JWT - Java Spring Boot

Microservicio central de la arquitectura SOA desarrollado en Java con Spring Boot, encargado del catálogo de vehículos, la emisión y validación de tokens JWT (`/oauth/token`), el control de acceso por roles (CONSULTA, OPERADOR, ADMIN), el manejo de reservas de vehículos para transacciones de venta y el soporte de dos instancias en balanceo ponderado 3:1 detrás de NGINX.

---

## 1. Requisitos Previos

* **Kit de desarrollo:** OpenJDK / JDK 17 o superior.
* **Gestor de construcción:** Apache Maven 3.9.0 o superior (o utilizar el wrapper `mvnw`).
* **Base de datos:** MySQL 8.0+ en XAMPP activo en el puerto estándar `3306`.
  * Nombre de base de datos: `venta_vehiculos`.
  * Usuario: `root`.
  * Contraseña: Vacía (`""`).
* **Dependencias de monitoreo:** Spring Boot Actuator para endpoint de salud `/actuator/health`.

---

## 2. Configuración

* **Archivo principal:** `src/main/resources/application.properties`.
* **Parámetros clave:**
  * `server.port=8080` (Instancia principal A) o `8082` (Instancia réplica B).
  * `spring.datasource.url=jdbc:mysql://localhost:3306/venta_vehiculos?useSSL=false&serverTimezone=UTC`.
  * `spring.datasource.username=root`.
  * `spring.datasource.password=` (compatible con instalación por defecto de XAMPP).
  * `spring.jpa.hibernate.ddl-auto=validate` (valida la estructura existente sin alterar datos).
  * `app.security.jwt-secret` (clave HMAC para firma y verificación stateless de JWT).
  * `app.security.access-token-minutes=30` (tiempo de vida del token).
  * `app.security.ventas-key=ventas-java-python-aula-2026-key-` (clave de integración con FastAPI).
* **Endpoints principales:**
  * `POST /oauth/token`: Autentica credenciales y emite JWT.
  * `GET /api/vehiculos`: Lista vehículos (permite filtro `?marca=...`).
  * `POST /api/vehiculos`: Da de alta un vehículo (requiere rol OPERADOR o ADMIN).
  * `POST /api/reservas`: Reserva temporal de vehículo para el flujo de ventas.
  * `GET /actuator/health`: Estado de salud expuesto para el monitor y NGINX.

---

## 3. Compilación

Para compilar las fuentes y empaquetar el proyecto mediante Maven:

```bash
# Navegar al directorio del proyecto
cd venta-vehiculos-api_security

# Compilación de clases
mvn clean compile -DskipTests

# Empaquetado en formato JAR
mvn clean package -DskipTests
```

---

## 4. Ejecución

La arquitectura exige dos instancias concurrentes para el balanceo y la tolerancia a fallos:

```bash
# Terminal 1: Iniciar Instancia A en puerto 8080 (por defecto)
mvn spring-boot:run

# Terminal 2: Iniciar Instancia B en puerto 8082
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"
```

O ejecutando directamente el archivo JAR generado:

```bash
# Instancia A
java -jar target/venta-vehiculos-api-0.0.1-SNAPSHOT.jar --server.port=8080

# Instancia B
java -jar target/venta-vehiculos-api-0.0.1-SNAPSHOT.jar --server.port=8082
```
