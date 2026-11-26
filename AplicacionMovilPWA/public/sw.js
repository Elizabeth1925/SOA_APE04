/**
 * ============================================================================
 * Archivo: public/sw.js
 * Propósito:
 *   Service Worker de la Aplicación Móvil PWA "AutoUTA". Gestiona el ciclo de
 *   vida del caché del cascarón (App Shell) para habilitar la instalación de la
 *   PWA y permitir abrir la interfaz sin conexión, aplicando una estrategia
 *   Network-First con respaldo en caché para recursos estáticos.
 *   Por política estricta de seguridad SOA, NUNCA intercepta ni almacena en
 *   caché peticiones dirigidas a `/api/` o `/oauth/`, ni cabeceras
 *   `Authorization` ni respuestas JSON sensibles del negocio.
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Service Worker y PWA instalable con aislamiento estricto de
 *     tráfico autenticado (`/api/`, `/oauth/`) para que el JWT y datos de
 *     ventas/vehículos nunca queden persistidos en Cache Storage.
 *   - [Actividad 12 - Mejora Visual AutoUTA v5] Actualización de versión del
 *     caché a `vehiculos-pwa-v5` e inclusión del activo gráfico
 *     `/icons/hero-auto-uta.png` del rediseño visual Minimalista Claro, con
 *     limpieza automática de cachés de versiones anteriores al activarse.
 * ============================================================================
 */

// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Identificador de versión del caché del Service Worker (v5 para forzar renovación de estilos y vistas AutoUTA v5).
const VERSION = 'vehiculos-pwa-v5';
// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Lista blanca de recursos estáticos del App Shell que se precachean para soporte PWA e interfaz offline.
const RECURSOS = [
  '/', '/index.html', '/styles.css', '/main.js', '/api.js',
  '/manifest.webmanifest', '/icons/icon-192.png', '/icons/icon-512.png', '/icons/hero-auto-uta.png'
];

// [Actividad 6] Evento `install`: se ejecuta al registrar una nueva versión del Service Worker para precargar el App Shell.
self.addEventListener('install', (evento) => {
  // [Actividad 6] Extiende la fase de instalación hasta completar el almacenamiento de todos los recursos estáticos.
  evento.waitUntil(
    // [Actividad 6] Abre (o crea) el contenedor de Cache Storage asociado a la versión actual `vehiculos-pwa-v5`.
    caches.open(VERSION)
      // [Actividad 6] Descarga y almacena atómicamente cada recurso definido en `RECURSOS`.
      .then((cache) => cache.addAll(RECURSOS))
      // [Actividad 12 - Mejora Visual AutoUTA v5] Fuerza que el nuevo Service Worker v5 pase a estado activo inmediatamente sin esperar el cierre de pestañas previas.
      .then(() => self.skipWaiting()),
  );
});

// [Actividad 6] Evento `activate`: limpia versiones antiguas de caché y toma control inmediato de los clientes abiertos.
self.addEventListener('activate', (evento) => {
  // [Actividad 6] Mantiene activa la fase hasta depurar todos los cachés obsoletos y reclamar las pestañas abiertas.
  evento.waitUntil(
    // [Actividad 12 - Mejora Visual AutoUTA v5] Obtiene todas las claves de caché existentes en el navegador.
    caches.keys().then((nombres) => Promise.all(
      // [Actividad 12 - Mejora Visual AutoUTA v5] Filtra las versiones distintas de `VERSION` (p. ej. v1..v4) y elimina cada caché obsoleto.
      nombres.filter((nombre) => nombre !== VERSION).map((nombre) => caches.delete(nombre)),
    // [Actividad 6] Reclama el control inmediato de todas las ventanas/clientes de la PWA bajo el mismo origen.
    )).then(() => self.clients.claim()),
  );
});

// [Actividad 6] Evento `fetch`: intercepta únicamente solicitudes GET estáticas del mismo origen, dejando pasar libremente toda la API y OAuth 2.0.
self.addEventListener('fetch', (evento) => {
  // [Actividad 6] Obtiene el objeto Request original emitido por el navegador.
  const solicitud = evento.request;
  // [Actividad 6] Analiza la URL solicitada para evaluar su origen y ruta (`pathname`).
  const url = new URL(solicitud.url);
  // [Actividad 6] Ignora cualquier petición que no sea GET (p. ej. POST /oauth/token o POST /api/ventas) o que pertenezca a otro origen.
  if (solicitud.method !== 'GET' || url.origin !== self.location.origin) return;

  // [Actividad 6] Regla crítica de seguridad: los endpoints protegidos (`/api/`) y de autenticación (`/oauth/`) siempre van directo a red y nunca se cachean.
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/oauth/')) return;

  // [Actividad 6] Para navegaciones de documento (`mode === 'navigate'`), aplica Network-First con respaldo offline en `/index.html`.
  if (solicitud.mode === 'navigate') {
    // [Actividad 6] Reemplaza la respuesta de navegación por la promesa de red con respaldo en caché.
    evento.respondWith(
      // [Actividad 6] Intenta descargar desde el servidor la versión más reciente de la pantalla principal.
      fetch(solicitud)
        .then((respuesta) => {
          // [Actividad 6] Clona el cuerpo de la respuesta HTTP porque su stream solo puede consumirse una vez.
          const copia = respuesta.clone();
          // [Actividad 6] Actualiza silenciosamente `/index.html` en el caché actual para futuras aperturas sin conexión.
          caches.open(VERSION).then((c) => c.put('/index.html', copia)).catch(() => {});
          // [Actividad 6] Devuelve al navegador la respuesta fresca obtenida de la red.
          return respuesta;
        })
        // [Actividad 6] Si no hay conectividad de red, entrega el `/index.html` previamente cacheado o un error de red estándar.
        .catch(async () => (await caches.match('/index.html')) || Response.error()),
    );
    // [Actividad 6] Finaliza el manejador porque la navegación ya fue respondida.
    return;
  }

  // [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Para recursos estáticos del App Shell listados en `RECURSOS`, usa Network-First con respaldo offline.
  if (RECURSOS.includes(url.pathname)) {
    // [Actividad 6] Intercepta la solicitud del activo estático para servir la versión en línea y refrescar su copia local.
    evento.respondWith(
      // [Actividad 6] Solicita primero el recurso actualizado al servidor local o HTTPS.
      fetch(solicitud)
        .then((respuesta) => {
          // [Actividad 6] Solo guarda en caché respuestas HTTP exitosas (2xx) para no almacenar errores temporales.
          if (respuesta.ok) {
            // [Actividad 6] Clona la respuesta antes de almacenarla en Cache Storage.
            const copia = respuesta.clone();
            // [Actividad 6] Persiste la versión fresca del activo bajo su `pathname` en el caché `vehiculos-pwa-v5`.
            caches.open(VERSION).then((c) => c.put(url.pathname, copia)).catch(() => {});
          }
          // [Actividad 6] Retorna al navegador el archivo descargado de la red.
          return respuesta;
        })
        // [Actividad 6] Si la red falla, recupera el recurso desde Cache Storage o devuelve `Response.error()`.
        .catch(async () => (await caches.match(url.pathname)) || Response.error()),
    );
  }
});
