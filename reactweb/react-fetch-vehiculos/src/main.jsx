/*
 * ============================================================================
 * Archivo: src/main.jsx
 * Proyecto: Portal Web React de Consulta y Auditoría (react-fetch-vehiculos) - SOA APE 04
 * Función:
 *   Punto de arranque de React en el navegador. Localiza el nodo DOM #root
 *   declarado en index.html y monta el componente raíz App dentro de
 *   React.StrictMode para detectar buenas prácticas y posibles efectos secundarios.
 * Actividades APE 04 cubiertas:
 *   - [Actividad 7]: Inicialización de la aplicación SPA React del portal web
 *     de consulta de vehículos y auditoría de ventas.
 * ============================================================================
 */
// Importa la biblioteca base React (requerida para acceder a React.StrictMode).
import React from 'react';
// Importa la API moderna createRoot de React 18+ para el renderizado concurrente en el DOM.
import { createRoot } from 'react-dom/client';
// Importa el componente principal App que encapsula la interfaz de consulta y auditoría [Actividad 7].
import App from './App.jsx';

// El elemento "root" se declara en index.html.
// createRoot permite que React dibuje y actualice la interfaz del portal web [Actividad 7].
createRoot(document.getElementById('root')).render(
  // Envuelve la aplicación en StrictMode para habilitar comprobaciones estrictas en modo desarrollo.
  <React.StrictMode>
    {/* Renderiza el componente raíz del portal web de consulta de vehículos y auditoría de ventas [Actividad 7] */}
    <App />
  </React.StrictMode>,
);
