/**
 * Service Worker: permite abrir la interfaz instalada sin red.
 * Por seguridad NUNCA intercepta /api, /oauth, Authorization ni las respuestas JSON.
 * Estrategia Network-First con respaldo en caché para asegurar actualizaciones inmediatas.
 */
const VERSION = 'vehiculos-pwa-v5';
const RECURSOS = [
  '/', '/index.html', '/styles.css', '/main.js', '/api.js',
  '/manifest.webmanifest', '/icons/icon-192.png', '/icons/icon-512.png', '/icons/hero-auto-uta.png'
];

self.addEventListener('install', (evento) => {
  evento.waitUntil(
    caches.open(VERSION)
      .then((cache) => cache.addAll(RECURSOS))
      .then(() => self.skipWaiting()),
  );
});

self.addEventListener('activate', (evento) => {
  evento.waitUntil(
    caches.keys().then((nombres) => Promise.all(
      nombres.filter((nombre) => nombre !== VERSION).map((nombre) => caches.delete(nombre)),
    )).then(() => self.clients.claim()),
  );
});

self.addEventListener('fetch', (evento) => {
  const solicitud = evento.request;
  const url = new URL(solicitud.url);
  if (solicitud.method !== 'GET' || url.origin !== self.location.origin) return;

  // Los datos protegidos y autenticación se obtienen siempre en línea; nunca se cachean.
  if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/oauth/')) return;

  // Al navegar, se intenta obtener una versión nueva; sin conexión, la pantalla almacenada.
  if (solicitud.mode === 'navigate') {
    evento.respondWith(
      fetch(solicitud)
        .then((respuesta) => {
          const copia = respuesta.clone();
          caches.open(VERSION).then((c) => c.put('/index.html', copia)).catch(() => {});
          return respuesta;
        })
        .catch(async () => (await caches.match('/index.html')) || Response.error()),
    );
    return;
  }

  // Recursos estáticos del shell PWA: Network-First con respaldo offline en caché.
  if (RECURSOS.includes(url.pathname)) {
    evento.respondWith(
      fetch(solicitud)
        .then((respuesta) => {
          if (respuesta.ok) {
            const copia = respuesta.clone();
            caches.open(VERSION).then((c) => c.put(url.pathname, copia)).catch(() => {});
          }
          return respuesta;
        })
        .catch(async () => (await caches.match(url.pathname)) || Response.error()),
    );
  }
});
