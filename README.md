# APE 04 - Arquitectura Multicanal de Vehículos, Compradores, Ventas y Notificaciones

**Asignatura:** Aplicaciones Orientadas a Servicios (SOA)  
**Nivel y Paralelo:** 5to "B"  
**Estudiante:** De la Cruz Llundo Carolina Elizabeth  
**Docente:** Ing. Javier Vargas, Mg.  
**Institución:** Universidad Técnica de Ambato (UTA) — FISEI, Carrera de Software  

---

## 1. Descripción General de la Arquitectura Final

Solución orientada a servicios (SOA) multicanal compuesta por **4 clientes especializados**, un **API Gateway con balanceo ponderado 3:1 (NGINX)**, **3 microservicios backend** (Spring Boot Vehículos con 2 instancias, Python FastAPI Ventas/Compradores y Spring Boot Notificaciones), un **broker asíncrono durable (RabbitMQ)**, **persistencia relacional separada por responsabilidad (MySQL)** y un **monitor de disponibilidad independiente (`monitor-balancer-service`)**.

```text
+--------------------+  +--------------------+  +--------------------+  +--------------------+
|   Windows Forms    |  |     PWA Móvil      |  |     React Web      |  |      JavaFX        |
|  C# / .NET 8       |  |  Node.js (:5173)   |  |  Vite/React (:5174)|  |   JDK 25 Desktop   |
|  Admin + Consulta  |  |  ÚNICO POST Ventas |  |  Solo Consulta     |  |  Catálogo + Avisos |
+---------+----------+  +---------+----------+  +---------+----------+  +---------+----------+
          |  REST/JWT             |  REST/JWT             |  REST/JWT             |  GET cada 3s
          +-----------------------+-----------------------+-----------------------+
                                               |
                                               v
                   +-------------------------------------------------------+
                   |           NGINX API GATEWAY (127.0.0.1:8090)          |
                   |  /oauth/token, /api/vehiculos -> Balanceo 3:1 (A/B)   |
                   |  /api/compradores, /api/ventas -> FastAPI (:8002)     |
                   |  /api/notificaciones -> Notificaciones (:8081)        |
                   |  /nginx-ping -> Diagnóstico local 200 UP              |
                   +-----------+-----------------------+-------------------+
                               |                       |                   |
            +------------------+--------+              |                   |
            | (weight=3)                | (weight=1)   |                   |
            v                           v              v                   v
  +-------------------+     +-------------------+  +---------------+  +----------------------+
  | Spring Boot (A)   |     | Spring Boot (B)   |  | FastAPI       |  | Notificaciones Java  |
  | 127.0.0.1:8080    |     | 127.0.0.1:8082    |  | 127.0.0.1:8002|  | 127.0.0.1:8081       |
  | OAuth2/JWT + Veh. |     | OAuth2/JWT + Veh. |  | Compr.+Ventas |  | @RabbitListener +    |
  +---------+---------+     +---------+---------+  +---+---+-------+  | GET /notificaciones  |
            |                         |                |   |          +----------+-----------+
            +------------+------------+                |   | AMQP durable        ^
                         |                             |   | delivery_mode=2     | Consume +
                         v                             v   v                     | Deduplica
               +------------------------------------------+  +-------------------+-----------+
               |           MySQL 8 / MariaDB (:3306)      |  |    RabbitMQ 3.13 (:5672)      |
               |  Java: vehiculos, usuarios_api, reservas |  | Cola: notificaciones.ventas   |
               |  Python: compradores, ventas             |  | Management UI: :15672         |
               +------------------------------------------+  +-------------------------------+

  Observabilidad independiente (fuera del flujo transaccional):
  [monitor-balancer-service :8083] -> /nginx-ping, :8080/actuator/health, :8082/actuator/health, access.log
```

---

## 2. Mapa Oficial de Puertos y Responsabilidades

| Componente | Tecnología | Puerto | Responsabilidad Obligatoria |
| :--- | :--- | :---: | :--- |
| **MySQL** | MariaDB / MySQL (XAMPP) | `3306` | Base `venta_vehiculos` con tablas separadas por servicio (`vehiculos`/`usuarios_api`/`reservas` vs `compradores`/`ventas`). |
| **RabbitMQ** | RabbitMQ 3.13 / Erlang 26 | `5672` / `15672` | Broker AMQP con cola durable `notificaciones.ventas` y mensajes persistentes (`delivery_mode=2`). |
| **NGINX Gateway** | NGINX 1.30.5 | `8090` | Punto único REST, balanceo ponderado `3:1` (A/B), enrutamiento a FastAPI y Notificaciones, `/nginx-ping` y `access.log` seguro (`$uri`). |
| **Vehículos (Inst. A)** | Spring Boot / JDK 25 | `8080` | Autenticación OAuth 2.0 / JWT stateless, CRUD de vehículos y reservas (`weight=3`). |
| **Vehículos (Inst. B)** | Spring Boot / JDK 25 | `8082` | Réplica equivalente para balanceo `3:1` y continuidad ante fallos (`weight=1`). |
| **Ventas API** | Python 3.12 / FastAPI | `8002` | Gestión de compradores, ventas en 2 fases con `Idempotency-Key` (SHA-256) y publicador AMQP + `worker.py`. |
| **Notificaciones** | Spring Boot 3.5 / Spring AMQP | `8081` | Consumidor `@RabbitListener`, deduplicación por `eventoId` y exposición de `GET /api/notificaciones`. |
| **Monitor NGINX** | Spring Boot | `8083` | Sondeo independiente cada 5 s (`/api/monitor/estado`, `/instancias`, `/balanceo`, `/estadisticas`, `/alertas`). |
| **Windows Forms** | C# / .NET 8 | Escritorio | Administración: crear/listar vehículos y compradores; consulta de ventas (sin `POST /api/ventas`). |
| **PWA Móvil** | Node.js / Vanilla JS / SW | `5173` | Login OAuth 2.0 (`/oauth/token`), catálogo y **único cliente con `POST /api/ventas`** e idempotencia. |
| **React Web** | Vite 8 / React 19 / Fetch | `5174` | Consulta de vehículos con filtro por marca, detalle técnico y auditoría de ventas (sin `POST /api/ventas`). |
| **JavaFX** | JavaFX 21 / JDK 25 | Escritorio | Catálogo de vehículos y monitor en vivo de notificaciones (sondeo cada 3 s, 3 estados de conexión). |

---

## 3. Estructura del Repositorio

```text
APE4/
├── AplicacionMovilPWA/             # Cliente PWA Móvil (Node.js :5173, Service Worker v2, OAuth2, POST /api/ventas)
├── ConsultaVehiculosForms/         # Cliente administrativo en C# .NET 8 (Vehículos, Compradores, Consulta Ventas)
├── escritorioFx/
│   └── consultarvehiculojavafx/    # Cliente JavaFX (Catálogo + Notificaciones en Vivo con sondeo cada 3 s)
├── reactweb/
│   └── react-fetch-vehiculos/      # Cliente Web React + Vite (:5174, Solo consulta y auditoría)
├── venta-vehiculos-api_security/   # Microservicio Spring Boot de Vehículos, OAuth2/JWT y Reservas (:8080 / :8082)
├── ventas-api/                     # Microservicio FastAPI de Compradores, Ventas idempotentes y RabbitMQ (:8002)
├── notificaciones-service/         # Microservicio Spring Boot consumidor RabbitMQ e historial (:8081)
├── monitor-balancer-service/       # Servicio independiente de monitoreo NGINX y métricas (:8083)
├── nginx/                          # Configuración nginx.conf (balanceo 3:1, failover y log sin query strings)
├── rabbitmq/                       # Definición reproducible de cola durable (definitions.json)
├── database/                       # Scripts SQL reproducibles (schema_base.sql y dump de referencia)
├── Documento/                      # Informe LaTeX, evidencias numeradas y guía práctica
├── INFORME_APE04_DELACRUZ_ELIZABETH.pdf
├── .gitignore
└── README.md
```

---

## 4. Preparación Local de Configuración (Sin Secretos en Repositorio ni ZIP)

Antes de ejecutar los servicios en un entorno limpio:
1. **Base de datos MySQL (`3306`):**
   - Ejecutar `database/schema_base.sql` en MySQL para crear la base `venta_vehiculos` y las tablas `usuarios_api`, `vehiculos` y `reservas`.
   - En `ventas-api`, crear `.env` copiando `ventas-api/.env.example` con las credenciales locales y ejecutar `alembic upgrade head` para crear `compradores` y `ventas`.
2. **Microservicios Java (`venta-vehiculos-api_security` y `notificaciones-service`):**
   - Copiar `src/main/resources/application.properties.example` a `src/main/resources/application.properties` en ambos servicios, configurando claves coherentes de mínimo 32 caracteres (`app.security.jwt-secret`, `app.security.ventas-key`, `app.notificacion-key`).
3. **RabbitMQ (`5672` / `15672`):**
   - Importar `rabbitmq/definitions.json` desde la consola web (`http://localhost:15672` → *Overview → Import definitions*) o permitir que `ventas-api` y `notificaciones-service` declaren automáticamente la cola durable `notificaciones.ventas`.
4. **NGINX (`8090`):**
   - Copiar `nginx/nginx.conf` a `C:\nginx\conf\nginx.conf` y validar sintaxis con `C:\nginx\nginx.exe -t`.

---

## 5. Orden Oficial de Arranque y Comandos Reproducibles

1. **MySQL (`:3306`):** Iniciar servicio MySQL desde XAMPP Control Panel.
2. **RabbitMQ (`:5672` / `:15672`):** Verificar servicio activo (`http://localhost:15672`).
3. **NGINX Gateway (`:8090`):**
   ```powershell
   cd C:\nginx
   .\nginx.exe
   ```
4. **API de Vehículos — Instancia A (`:8080`):**
   ```powershell
   cd venta-vehiculos-api_security
   mvn clean package -DskipTests
   java -jar target/venta-vehiculos-api-0.0.1-SNAPSHOT.jar
   ```
5. **API de Vehículos — Instancia B (`:8082`):**
   ```powershell
   cd venta-vehiculos-api_security
   java -jar target/venta-vehiculos-api-0.0.1-SNAPSHOT.jar --server.port=8082
   ```
6. **API de Ventas — Python FastAPI (`:8002`):**
   ```powershell
   cd ventas-api
   python -m venv .venv
   .\.venv\Scripts\Activate.ps1
   pip install -r requirements.txt
   alembic upgrade head
   uvicorn app.main:app --host 127.0.0.1 --port 8002 --reload
   ```
   *(Proceso opcional de conciliación/reintentos pendientes ante caída de RabbitMQ: `python -m app.worker --once`.)*
7. **Servicio de Notificaciones (`:8081`):**
   ```powershell
   cd notificaciones-service
   mvn clean package -DskipTests
   java -jar target/notificaciones-service-0.0.1-SNAPSHOT.jar
   ```
8. **Monitor de Balanceo Independiente (`:8083`, opcional):**
   ```powershell
   cd monitor-balancer-service
   mvn clean package -DskipTests
   java -jar target/monitor-balancer-service-0.0.1-SNAPSHOT.jar
   ```
9. **Clientes Multicanal:**
   - **Windows Forms (.NET 8):**
     ```powershell
     cd ConsultaVehiculosForms\ConsultaVehiculosForms
     dotnet run
     ```
   - **PWA Móvil (`http://localhost:5173`):**
     ```powershell
     cd AplicacionMovilPWA
     npm test
     node server.mjs
     ```
   - **React Web (`http://localhost:5174`):**
     ```powershell
     cd reactweb\react-fetch-vehiculos
     npm install
     npm run dev
     ```
   - **JavaFX Desktop:**
     ```powershell
     cd escritorioFx\consultarvehiculojavafx
     mvn javafx:run
     ```

---

## 6. Reglas de Seguridad y Exclusiones de Entrega

- Ningún archivo de repositorio o del paquete ZIP incluye dependencias descargadas (`node_modules`, `target`, `bin`, `obj`, `.venv`, `.git`), archivos `.env` locales, `application.properties` con secretos, contraseñas de producción ni tokens JWT reales.
- Todas las peticiones de negocio y consulta circulan por NGINX (`:8090`); los tokens JWT permanecen únicamente en memoria volátil del cliente durante la sesión activa.
