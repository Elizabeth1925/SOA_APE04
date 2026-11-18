# Cliente Administrativo Windows Forms - Gestión de Vehículos, Compradores y Ventas

Aplicación de escritorio para Windows desarrollada en C# y .NET 8, destinada a las operaciones administrativas del sistema: creación y listado de vehículos (Java Spring Boot), creación y listado de compradores (Python FastAPI) y consulta de ventas realizadas.

---

## 1. Requisitos Previos

* **Sistema Operativo:** Windows 10 o Windows 11 (arquitectura x64).
* **SDK de desarrollo:** .NET SDK 8.0 o superior (compatible con .NET 10).
* **Entorno de desarrollo sugerido:** Visual Studio 2022 (con carga de trabajo de desarrollo de escritorio de .NET) o Visual Studio Code con C# Dev Kit.
* **Servicios backend necesarios:**
  * Gateway NGINX activo en `http://127.0.0.1:8090/`.
  * Base de datos MySQL en XAMPP activa con esquema `venta_vehiculos`.
  * Microservicio de Vehículos y Microservicio de Ventas en ejecución.

---

## 2. Configuración

* **Punto de conexión a la API:** Configurado en las clases `VehiculoApiClient.cs` y `VentaApiClient.cs` para consumir los endpoints a través del Gateway NGINX en `http://127.0.0.1:8090/`.
* **Endpoints consumidos:**
  * `POST /api/vehiculos` y `GET /api/vehiculos` (Gestión de vehículos).
  * `POST /api/compradores` y `GET /api/compradores` (Gestión de compradores).
  * `GET /api/ventas` y `GET /api/ventas/{id}` (Consulta de ventas).
* **Autenticación:** Las solicitudes HTTP incorporan la cabecera `Authorization: Bearer <token_jwt>` ingresada en el campo de control correspondiente.
* **Manejo de códigos HTTP:** Presenta estados explícitos de éxito (200, 201) y captura errores 400, 401, 403, 404, 409 y fallos de conexión.

---

## 3. Compilación

Para restaurar dependencias NuGet y compilar el proyecto en modo depuración o producción mediante la CLI de .NET:

```powershell
# Navegar al directorio de la solución
cd ConsultaVehiculosForms

# Restaurar paquetes NuGet
dotnet restore

# Compilar el proyecto en modo Release
dotnet build --configuration Release
```

---

## 4. Ejecución

Para iniciar la aplicación desde la terminal de comandos:

```powershell
# Ejecutar el proyecto mediante dotnet CLI
dotnet run --project ConsultaVehiculosForms/ConsultaVehiculosForms.csproj
```

O abrir la solución `ConsultaVehiculosForms.slnx` en Visual Studio 2022 y presionar `F5`.
