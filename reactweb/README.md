# Plataforma Web React - Consulta de Vehículos y Reporte de Ventas

Cliente web para navegador desarrollado con React 19, Vite y Fetch API, diseñado para la consulta del catálogo de vehículos con filtrado por marca y la visualización de reportes e indicadores de ventas reales dentro de la arquitectura SOA.

---

## 1. Requisitos Previos

* **Entorno de ejecución:** Node.js v20.0.0 o superior (compatible con v24 LTS).
* **Gestor de dependencias:** npm v10.0.0 o superior.
* **Navegador web compatible:** Google Chrome, Mozilla Firefox, Microsoft Edge o Safari (versiones modernas con soporte ES Modules).
* **Servicios backend necesarios:**
  * Gateway NGINX activo en `http://127.0.0.1:8090/`.
  * Catálogo de vehículos y API de ventas en ejecución.

---

## 2. Configuración

* **Ubicación del código fuente:** Subdirectorio `react-fetch-vehiculos/`.
* **Puerto de desarrollo:** `5173` (definido en `vite.config.js`).
* **Endpoints consumidos:**
  * `GET /api/vehiculos`: Consulta y listado del catálogo de vehículos.
  * `GET /api/ventas`: Consulta del listado y resumen de ventas efectuadas.
* **Seguridad y tokens:** El token JWT se mantiene en el estado de memoria del componente React (`useState`), evitando persistencia en archivos o almacenamiento web vulnerable.
* **Restricción de negocio:** Por especificación arquitectónica, este cliente web actúa estrictamente como plataforma de consulta e indicadores y no implementa formularios de registro de ventas.

---

## 3. Compilación e Instalación

Para instalar las dependencias de Node y compilar el proyecto para producción:

```bash
# Navegar al directorio de la aplicación React
cd reactweb/react-fetch-vehiculos

# Instalar dependencias requeridas (React, Vite, plugins)
npm install

# Compilar para producción (genera bundle optimizado en carpeta /dist)
npm run build
```

---

## 4. Ejecución

Para iniciar el servidor de desarrollo de Vite:

```bash
# Iniciar servidor local de desarrollo
npm run dev
```

* **Acceso desde el navegador:** Abrir [http://localhost:5173](http://localhost:5173).
* Para previsualizar el build de producción: `npm run preview`.
