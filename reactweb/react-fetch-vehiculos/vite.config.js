/*
 * ============================================================================
 * Archivo: vite.config.js
 * Proyecto: Portal Web React de Consulta y Auditoría (react-fetch-vehiculos) - SOA APE 04
 * Función:
 *   Configura el servidor de desarrollo y empaquetador Vite del portal web
 *   React, asignando un puerto dedicado (:5174) para evitar colisiones con la
 *   PWA (:5173) y enrutando las peticiones /api y /oauth mediante proxy hacia
 *   el Gateway NGINX (:8090).
 * Actividades APE 04 cubiertas:
 *   - [Actividad 7]: Ejecución del portal de consulta y auditoría en el puerto
 *     estricto 5174 (sin conflicto con PWA :5173) y proxy Vite hacia NGINX
 *     (:8090) para consumir GET /api/vehiculos y GET /api/ventas sin CORS.
 * ============================================================================
 */
// Importa la función auxiliar defineConfig de Vite para declarar la configuración con tipado asistido.
import { defineConfig } from 'vite';
// Importa el plugin oficial de React para Vite encargado de transformar JSX y habilitar Fast Refresh.
import react from '@vitejs/plugin-react';

// Exporta la configuración principal de Vite para desarrollo, proxy HTTP y vista previa [Actividad 7].
export default defineConfig({
  // Registra el plugin de React en el pipeline de compilación de Vite.
  plugins: [react()],
  // Configuración del servidor de desarrollo local del portal web React [Actividad 7].
  server: {
    // Puerto dedicado 5174 para operar en paralelo sin conflicto con la PWA (:5173) [Actividad 7].
    port: 5174,
    // Exige usar exactamente el puerto 5174 (falla en lugar de saltar a otro puerto libre) [Actividad 7].
    strictPort: true,
    // Reglas de proxy inverso de desarrollo para redirigir rutas relativas hacia el Gateway NGINX (:8090) [Actividad 7].
    proxy: {
      // Redirige /api/* (GET /api/vehiculos y GET /api/ventas) al Gateway NGINX en 127.0.0.1:8090 [Actividad 7].
      '/api': {
        // Dirección base del Gateway NGINX que balancea Spring Boot y enruta FastAPI.
        target: 'http://127.0.0.1:8090',
        // Reescribe la cabecera Host al destino NGINX para evitar rechazos de origen/virtual host.
        changeOrigin: true,
      },
      // Redirige /oauth/* al Gateway NGINX en 127.0.0.1:8090 para flujos de autenticación OAuth 2.0.
      '/oauth': {
        // Dirección base del Gateway NGINX para endpoints de autorización.
        target: 'http://127.0.0.1:8090',
        // Ajusta la cabecera Host al servidor destino.
        changeOrigin: true,
      },
    },
  },
  // Configuración del servidor local de previsualización de la compilación de producción (npm run preview).
  preview: {
    // Puerto asignado para previsualizar el bundle compilado en dist/.
    port: 4173,
    // Impide el cambio automático de puerto si 4173 ya está ocupado.
    strictPort: true,
  },
});
