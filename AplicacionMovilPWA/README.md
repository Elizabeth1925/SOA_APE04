# Aplicación Móvil PWA - Gestión y Registro de Ventas

Cliente móvil desarrollado como Progressive Web App (PWA) con HTML5, CSS3 y JavaScript moderno, diseñado para la autenticación de usuarios con JWT y el registro exclusivo de ventas de vehículos dentro de la arquitectura multicanal SOA.

---

## 1. Requisitos Previos

* **Entorno de ejecución:** Node.js v20.0.0 o superior (compatible con v24 LTS).
* **Gestor de paquetes:** npm v10.0.0 o superior.
* **Navegador web compatible:** Google Chrome, Microsoft Edge, Mozilla Firefox o Safari con soporte para PWA y Service Workers.
* **Servicios backend necesarios:**
  * NGINX Gateway activo en `http://127.0.0.1:8090`.
  * Microservicio de Vehículos (Spring Boot) activo para autenticación `/oauth/token`.
  * Microservicio de Ventas (FastAPI) activo para transacciones `/api/ventas`.

---

## 2. Configuración

* **Archivo de servidor:** `server.mjs`.
* **Puerto local de desarrollo:** `5174` (para no entrar en conflicto con React en `5173`).
* **Punto de enlace API:** Todas las solicitudes REST (`/oauth/token`, `/api/vehiculos`, `/api/compradores`, `/api/ventas`) se enrutan mediante el Gateway NGINX en `http://127.0.0.1:8090`.
* **Seguridad de sesión:** El token JWT obtenido se preserva exclusivamente en memoria durante la sesión del navegador, sin almacenarse en almacenamiento local (`localStorage`) ni en cookies no seguras.

---

## 3. Compilación y Preparación

Al ser una aplicación web estática con Service Worker, no requiere un paso de transpilación obligatorio para desarrollo, pero dispone de scripts de verificación y empaquetado para distribución:

```bash
# Navegar al directorio de la aplicación
cd AplicacionMovilPWA

# Ejecutar pruebas unitarias de cliente y servidor
npm test

# Generar empaquetado optimizado en carpeta /dist (opcional)
npm run build
```

---

## 4. Ejecución

Para iniciar el servidor local de la PWA:

```bash
# Iniciar en modo desarrollo en el puerto 5174
node server.mjs --port=5174

# O mediante npm:
npm start
```

* **Acceso desde el navegador:** Abrir [http://localhost:5174](http://localhost:5174).
* **Instalación como PWA:** En Chrome o Edge, hacer clic en el ícono de instalación en la barra de direcciones para ejecutarla en modo aplicación independiente.
