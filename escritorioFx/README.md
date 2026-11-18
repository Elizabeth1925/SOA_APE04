# Cliente de Notificaciones JavaFX - Monitoreo de Ventas en Tiempo Real

Aplicación de escritorio multiplataforma desarrollada en Java con JavaFX, encargada de consultar y visualizar de manera reactiva y periódica las notificaciones de ventas emitidas por el sistema multicanal SOA.

---

## 1. Requisitos Previos

* **Kit de desarrollo:** OpenJDK / JDK 17 o superior (compatible con JDK 25 LTS).
* **Gestor de construcción:** Apache Maven 3.9.0 o superior.
* **Librerías gráficas:** OpenJFX (JavaFX SDK 21+ gestionado automáticamente vía Maven).
* **Servicios backend necesarios:**
  * Microservicio de Notificaciones (Spring Boot) activo en `http://127.0.0.1:8081`.
  * RabbitMQ activo procesando la cola de mensajes `notificaciones.ventas`.

---

## 2. Configuración

* **Ubicación del proyecto:** Subdirectorio `consultarvehiculojavafx/`.
* **Archivo de configuración Maven:** `pom.xml`.
* **Endpoint de notificaciones:** Configurado en `VehiculosApiClient.java` para consultar `http://127.0.0.1:8081/api/notificaciones`.
* **Mecanismo de refresco:** Sondeo automático (polling) configurable cada 3 a 5 segundos sin intervención manual reiterada del usuario.
* **Tolerancia a fallos:** Distingue visualmente entre una lista vacía de notificaciones y una pérdida de conectividad con el microservicio.

---

## 3. Compilación

Para compilar las clases y empaquetar el proyecto JavaFX utilizando Maven:

```bash
# Navegar al subdirectorio del proyecto JavaFX
cd escritorioFx/consultarvehiculojavafx

# Limpiar y compilar el proyecto
mvn clean compile -DskipTests

# Opcional: Generar paquete JAR ejecutable
mvn clean package -DskipTests
```

---

## 4. Ejecución

Para iniciar la aplicación JavaFX desde la línea de comandos con el plugin oficial de JavaFX:

```bash
# Iniciar mediante el plugin de Maven JavaFX
mvn javafx:run
```

O ejecutando la clase principal `Launcher` directamente desde el entorno de desarrollo (VS Code / IntelliJ IDEA / Eclipse).
