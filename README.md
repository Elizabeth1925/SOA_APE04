# APE 04 - Arquitectura Multicanal de Vehículos, Compradores, Ventas y Notificaciones

**Asignatura:** Aplicaciones Orientadas a Servicios (SOA)  
**Nivel y Paralelo:** 5to "B"  
**Estudiante:** De la Cruz Llundo Carolina Elizabeth  
**Docente:** Ing. Javier Vargas, Mg.  
**Universidad Técnica de Ambato**  

---

## 1. Descripción General del Sistema
Integración de una arquitectura orientada a servicios (SOA) multicanal compuesta por 4 aplicaciones cliente (Windows Forms, PWA Móvil, React Web y JavaFX Desktop) y 3 servicios de backend (Spring Boot y FastAPI), con balanceo ponderado en NGINX, transporte asíncrono de eventos con RabbitMQ y persistencia relacional en MySQL.

```
                           +-------------------------------------+
                           |            CLIENTES SOA             |
                           +-------------------------------------+
                             |           |          |         |
           [Windows Forms] --+           |          |         +-- [JavaFX]
             (Admin C#)                  |          |            (Avisos)
                               [PWA Móvil]          [React Web]       |
                               (Venta JWT)           (Consulta)       |
                                     |                    |           |
                                     v                    v           |
                       +----------------------------------------+     |
                       |       NGINX REST GATEWAY (:8090)       |     |
                       +----------------------------------------+     |
                         /              |                     \       |
     (3:1 Weighted RR)  /               |                      \      |
                       v                v                       v     v
             +------------------+  +------------------+  +------------------+
             | API Vehículos    |  | API Ventas       |  | Notificaciones   |
             | Java Spring Boot |  | Python FastAPI   |  | Java Spring Boot |
             | Instancia A:8080 |  | Puerto: 8002     |  | Puerto: 8081     |
             | Instancia B:8082 |  | (RabbitMQ Pub)   |  | (RabbitMQ Sub)   |
             +------------------+  +------------------+  +------------------+
                       |                    |                     ^
                       v                    v                     |
             +------------------+           +-------[ RabbitMQ ]--+
             | MySQL Database   |                    (5672)
             | :3306            |             (Cola: notificaciones.ventas)
             +------------------+
```

---

## 2. Mapa de Puertos y Servicios

| Componente | Tecnología | Puerto | Responsabilidad Obligatoria |
| :--- | :--- | :--- | :--- |
| **NGINX Gateway** | NGINX 1.27+ | `8090` | Punto único de entrada REST, balanceo ponderado 3:1 de vehículos, proxy a ventas y notificaciones. |
| **Vehículos (A)** | Spring Boot 4 / Java 17+ | `8080` | Catálogo de vehículos, autenticación OAuth2/JWT y reservas. Instancia principal. |
| **Vehículos (B)** | Spring Boot 4 / Java 17+ | `8082` | Instancia réplica para balanceo y tolerancia a fallos. |
| **Ventas API** | Python 3.12+ / FastAPI | `8002` | Gestión de compradores, ventas idempotentes (SHA-256) y publicación de eventos RabbitMQ. |
| **Notificaciones**| Spring Boot 3 / Java 17+ | `8081` | Consumidor RabbitMQ (`notificaciones.ventas`), historial y API de avisos. |
| **Monitor NGINX** | Spring Boot / Java 17+ | `8083` | Supervisión de salud (Actuator UP) y estadísticas con ventana temporal. |
| **RabbitMQ** | Erlang / RabbitMQ 3.13+ | `5672` / `15672`| Broker AMQP durable y panel de administración. |
| **MySQL** | MySQL 8.0+ / XAMPP | `3306` | Base de datos relacional `venta_vehiculos`. |
| **Windows Forms** | .NET 8 / C# | Desktop | Administración: crear/listar vehículos, compradores y listar ventas. |
| **PWA Móvil** | Node.js / Vanilla JS | `5174` | Login con credenciales, obtención de JWT, registro exclusivo de ventas con idempotencia. |
| **React Web** | Vite / React 19 | `5173` | Consulta de catálogo con filtros y reporte de ventas. |
| **JavaFX** | JavaFX 25 / OpenJDK | Desktop | Visualización reactiva de notificaciones de ventas en tiempo real. |

---

## 3. Estructura del Repositorio

```
APE4/
├── AplicacionMovilPWA/             # Cliente PWA Móvil (HTML5, JS, PWA)
├── ConsultaVehiculosForms/         # Cliente administrativo en C# .NET 8
├── Documento/                      # Guía práctica oficial y formatos de informe
├── escritorioFx/                   # Cliente de notificaciones JavaFX
│   └── consultarvehiculojavafx/
├── notificaciones-service/         # Microservicio Spring Boot de avisos y RabbitMQ listener
├── reactweb/                       # Cliente web React + Vite
│   └── react-fetch-vehiculos/
├── venta-vehiculos-api_security/   # Microservicio Spring Boot de vehículos y JWT
├── ventas-api/                     # Microservicio Python FastAPI de ventas y RabbitMQ publisher
├── monitor-balancer-service/       # Servicio independiente de monitoreo NGINX
├── nginx/                          # Configuración de NGINX con métricas seguras
├── database/                       # Scripts SQL y migraciones de esquema
├── .gitignore                      # Reglas de exclusión para control de versiones limpio
└── README.md                       # Documentación principal de la práctica
```

---

## 4. Orden de Arranque del Sistema

1. **MySQL** (`localhost:3306`): Iniciar servicio con la base `venta_vehiculos`.
2. **RabbitMQ** (`localhost:5672` / `localhost:15672`): Iniciar broker de mensajería.
3. **NGINX** (`localhost:8090`): Ejecutar `nginx.exe -c <ruta_a_nginx.conf>`.
4. **API de Vehículos (Instancia A)**: `mvn spring-boot:run` en puerto `8080`.
5. **API de Vehículos (Instancia B)**: `mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"`.
6. **API de Ventas (FastAPI)**: Iniciar con `uvicorn app.main:app --port 8002 --reload`.
7. **Servicio de Notificaciones**: Iniciar con `mvn spring-boot:run` en puerto `8081`.
8. **Monitor de Balanceo (Opcional)**: Iniciar con `mvn spring-boot:run` en puerto `8083`.
9. **Clientes**:
   * Windows Forms: Compilar y ejecutar desde Visual Studio o `dotnet run`.
   * PWA Móvil: Iniciar servidor local con `node server.mjs`.
   * React Web: Iniciar entorno de desarrollo con `npm run dev`.
   * JavaFX: Ejecutar con `mvn javafx:run`.
