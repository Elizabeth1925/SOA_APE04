/**
 * ============================================================================
 * Archivo: server.mjs
 * Propósito:
 *   Servidor HTTP local sin dependencias externas (Node.js puro) para la
 *   Aplicación Móvil PWA "AutoUTA". Cumple una doble función:
 *     1. Servir los archivos estáticos de la PWA (`public/` o `dist/`) con
 *        cabeceras de seguridad y protección contra path traversal.
 *     2. Actuar como proxy inverso same-origin para todas las rutas `/api/*` y
 *        `/oauth/*`, reenviándolas hacia el Gateway NGINX (`http://127.0.0.1:8090`
 *        por defecto), filtrando cabeceras hop-by-hop y propagando intactas las
 *        cabeceras de negocio (`Authorization`, `Idempotency-Key`) y códigos
 *        HTTP reales del backend.
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Proxy inverso same-origin hacia NGINX (:8090) para evitar
 *     problemas CORS en el navegador, filtro de cabeceras hop-by-hop, reenvío
 *     de `POST /oauth/token`, `GET /api/vehiculos`, `GET /api/compradores`,
 *     `POST /api/ventas` (con `Idempotency-Key`) y `GET /api/ventas/{id}`,
 *     más entrega estática del App Shell PWA y Service Worker.
 *   - [Actividad 11 - P08/P10/P11/P12/P13/503/504] Propagación fiel de códigos
 *     HTTP reales del backend (200/201/401/403/404/409/503) y generación
 *     controlada de HTTP 504 (timeout de 15 s) y HTTP 502 (gateway/API caída)
 *     en formato JSON sin inventar respuestas exitosas.
 * ============================================================================
 */

// [Actividad 6] Importa `createServer` del módulo nativo `node:http` para crear el servidor HTTP sin dependencias externas.
import { createServer } from 'node:http';
// [Actividad 6] Importa `readFile` y `stat` para leer archivos estáticos y validar su existencia/tipo.
import { readFile, stat } from 'node:fs/promises';
// [Actividad 6] Importa utilidades de rutas (`resolve`, `extname`, `sep`) para resolver activos y prevenir traversal de directorios.
import { resolve, extname, sep } from 'node:path';
// [Actividad 6] Importa `fileURLToPath` para obtener el directorio raíz del módulo ESM actual.
import { fileURLToPath } from 'node:url';

// [Actividad 6] Calcula la ruta absoluta raíz del proyecto donde reside `server.mjs`.
const ROOT = resolve(fileURLToPath(new URL('.', import.meta.url)));
// [Actividad 6] Mapa de tipos MIME permitidos para servir correctamente HTML, CSS, módulos ES, iconos y manifiesto PWA.
const TIPOS = {
  '.html': 'text/html; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8', '.png': 'image/png',
  '.webmanifest': 'application/manifest+json; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.svg': 'image/svg+xml',
};

// [Actividad 6] Fábrica exportada que crea la instancia HTTP del servidor PWA + proxy inverso hacia NGINX (también reutilizada por pruebas).
export function crearServidor({
  // [Actividad 6] Carpeta desde donde se sirven los archivos estáticos (`public/` por defecto o `dist/` en preview).
  directorio = resolve(ROOT, 'public'),
  // [Actividad 6] URL base del Gateway NGINX SOA (`http://127.0.0.1:8090` por defecto, sobreescribible con `API_BASE_URL`).
  apiBase = process.env.API_BASE_URL || 'http://127.0.0.1:8090',
} = {}) {
  // [Actividad 6] Normaliza la ruta absoluta de la carpeta estática raíz para comparaciones seguras.
  const carpeta = resolve(directorio);

  // [Actividad 6] Crea el servidor HTTP y define el manejador asíncrono por cada petición entrante.
  return createServer(async (peticion, respuesta) => {
    // [Actividad 6] Parsea la URL relativa de la petición (ruta + query string) usando una base ficticia.
    const url = new URL(peticion.url || '/', 'http://localhost');
    // [Actividad 6] Cabecera de seguridad: impide al navegador adivinar (MIME sniffing) el tipo de contenido.
    respuesta.setHeader('X-Content-Type-Options', 'nosniff');
    // [Actividad 6] Cabecera de seguridad: evita filtrar URLs de origen en la cabecera Referer.
    respuesta.setHeader('Referrer-Policy', 'no-referrer');
    // [Actividad 6] Cabecera de seguridad: deshabilita caché HTTP por defecto para servir cambios frescos y no retener datos sensibles.
    respuesta.setHeader('Cache-Control', 'no-store');

    // [Actividad 6] Bloque proxy inverso same-origin: intercepta toda petición dirigida a `/api/` o `/oauth/` y la reenvía hacia NGINX.
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/oauth/')) {
      // [Actividad 11 - 504] Crea un controlador de aborto para imponer un tiempo máximo de espera hacia NGINX/backends.
      const abortador = new AbortController();
      // [Actividad 11 - 504] Programa el corte automático de la solicitud saliente tras 15 000 ms (15 segundos) para devolver HTTP 504.
      const temporizador = setTimeout(() => abortador.abort(), 15000);
      try {
        // [Actividad 6] Conjunto de cabeceras hop-by-hop (RFC 9110) que NO deben reenviarse entre saltos proxy para evitar corrupción HTTP.
        const cabecerasExcluidas = new Set([
          'host', 'connection', 'keep-alive', 'transfer-encoding', 'upgrade',
          'expect', 'content-length', 'proxy-connection', 'te', 'trailer',
        ]);
        // [Actividad 6] Diccionario donde se copiarán las cabeceras válidas de extremo a extremo (incluyendo `Authorization`, `Idempotency-Key`, `Content-Type`).
        const encabezados = {};
        // [Actividad 6] Recorre cada cabecera de la petición entrante para filtrar únicamente las permitidas.
        for (const [clave, valor] of Object.entries(peticion.headers)) {
          // [Actividad 6] Conserva la cabecera solo si no pertenece a la lista hop-by-hop y posee un valor definido.
          if (!cabecerasExcluidas.has(clave.toLowerCase()) && valor !== undefined) {
            encabezados[clave] = valor;
          }
        }
        // [Actividad 6] Si el cliente no envió `Accept`, establece `application/json` por defecto para los servicios REST.
        if (!encabezados.accept) encabezados.accept = 'application/json';

        // [Actividad 6] Lee el cuerpo binario completo en peticiones con payload (POST /oauth/token, POST /api/ventas, PUT, etc.).
        let cuerpo = undefined;
        // [Actividad 6] Solo lee el stream del cuerpo cuando el método HTTP admite payload (distinto de GET y HEAD).
        if (peticion.method !== 'GET' && peticion.method !== 'HEAD') {
          // [Actividad 6] Acumula los fragmentos (chunks) recibidos del cuerpo HTTP.
          const trozos = [];
          // [Actividad 6] Itera asíncronamente sobre el stream de la petición entrante.
          for await (const trozo of peticion) {
            // [Actividad 6] Almacena cada fragmento de bytes en el arreglo temporal.
            trozos.push(trozo);
          }
          // [Actividad 6] Si se recibió contenido, concatena todos los fragmentos en un único `Buffer`.
          if (trozos.length > 0) {
            cuerpo = Buffer.concat(trozos);
          }
        }

        // [Actividad 6] Construye la URL absoluta de destino en NGINX preservando ruta y parámetros de consulta (`url.search`).
        const destino = `${apiBase.replace(/\/$/, '')}${url.pathname}${url.search}`;
        // [Actividad 6] Ejecuta el reenvío HTTP real hacia el Gateway NGINX con el método, cabeceras filtradas, cuerpo y señal de timeout.
        const remoto = await fetch(destino, {
          method: peticion.method,
          headers: encabezados,
          body: cuerpo,
          signal: abortador.signal,
          cache: 'no-store',
        });

        // [Actividad 6] Prepara las cabeceras de respuesta al navegador garantizando `Cache-Control: no-store`.
        const cabecerasRespuesta = {
          'Cache-Control': 'no-store',
        };
        // [Actividad 6] Recupera el `Content-Type` devuelto por NGINX/Spring Boot/FastAPI (p. ej. `application/json` o `application/problem+json`).
        const tipo = remoto.headers.get('content-type');
        // [Actividad 6] Si el servicio remoto informó un tipo de contenido, lo propaga tal cual al cliente PWA.
        if (tipo) cabecerasRespuesta['Content-Type'] = tipo;

        // [Actividad 11 - P08/P10/P11/P12/P13/503/504] Escribe exactamente el código HTTP real devuelto por NGINX/backend (200, 201, 401, 403, 409, 503, etc.).
        respuesta.writeHead(remoto.status, cabecerasRespuesta);
        // [Actividad 6] Reenvía el cuerpo exacto de la respuesta remota convertido a `Buffer` sin alteraciones.
        respuesta.end(Buffer.from(await remoto.arrayBuffer()));
      } catch (error) {
        // [Actividad 11 - 503/504] Distingue si el fallo ocurrió por agotamiento del temporizador de 15 s (`AbortError` -> 504) o caída de conexión (`502`).
        const tiempoAgotado = error.name === 'AbortError';
        // [Actividad 11 - 503/504] Responde HTTP 504 (Gateway Timeout) o HTTP 502 (Bad Gateway) en JSON explícito sin simular éxito.
        respuesta.writeHead(tiempoAgotado ? 504 : 502, { 'Content-Type': 'application/json; charset=utf-8' });
        // [Actividad 11 - 503/504] Serializa el mensaje descriptivo del fallo de infraestructura para que la PWA lo muestre al usuario.
        respuesta.end(JSON.stringify({ error: tiempoAgotado ? 'Tiempo de espera agotado' : 'Gateway NGINX o API no disponible' }));
      } finally {
        // [Actividad 11 - 504] Cancela siempre el temporizador para evitar fugas de timers en Node.js.
        clearTimeout(temporizador);
      }
      // [Actividad 6] Finaliza el manejo de la petición porque ya fue despachada por el proxy inverso.
      return;
    }

    // [Actividad 6] Para archivos estáticos de la PWA, únicamente se admiten métodos GET y HEAD; cualquier otro recibe HTTP 405.
    if (peticion.method !== 'GET' && peticion.method !== 'HEAD') {
      respuesta.writeHead(405); respuesta.end(); return;
    }

    // [Actividad 6] Variable que almacenará la ruta decodificada del recurso estático solicitado.
    let ruta;
    // [Actividad 6] Decodifica caracteres escapados de la URL; si la secuencia es malformada, responde HTTP 400.
    try { ruta = decodeURIComponent(url.pathname); }
    catch { respuesta.writeHead(400); respuesta.end(); return; }
    // [Actividad 6] Resuelve la ruta absoluta en disco mapeando `/` hacia `/index.html`.
    const archivo = resolve(carpeta, '.' + (ruta === '/' ? '/index.html' : ruta));
    // [Actividad 6] Control de seguridad anti Path Traversal: bloquea con HTTP 403 cualquier intento de salir de la carpeta estática autorizada.
    if (!(archivo === carpeta || archivo.startsWith(carpeta + sep))) {
      respuesta.writeHead(403); respuesta.end(); return;
    }
    try {
      // [Actividad 6] Obtiene metadatos del archivo en disco para confirmar su existencia.
      const detalle = await stat(archivo);
      // [Actividad 6] Rechaza directorios u objetos que no sean archivos regulares disparando el bloque `catch` (HTTP 404).
      if (!detalle.isFile()) throw new Error('Archivo no encontrado');
      // [Actividad 6] Asigna el encabezado `Content-Type` correspondiente a la extensión del archivo (o binario genérico).
      respuesta.setHeader('Content-Type', TIPOS[extname(archivo)] || 'application/octet-stream');
      // [Actividad 6] Si el método es HEAD, devuelve solo los encabezados con estado HTTP 200 sin leer el archivo completo.
      if (peticion.method === 'HEAD') { respuesta.writeHead(200); respuesta.end(); return; }
      // [Actividad 6] Envía estado HTTP 200 OK para la descarga del archivo estático de la PWA.
      respuesta.writeHead(200);
      // [Actividad 6] Lee el archivo desde disco y lo transmite al navegador.
      respuesta.end(await readFile(archivo));
    } catch {
      // [Actividad 6] Cuando el recurso estático no existe, responde HTTP 404 en texto plano UTF-8.
      respuesta.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
      // [Actividad 6] Entrega el mensaje estándar de recurso no encontrado.
      respuesta.end('Recurso no encontrado');
    }
  });
}

// [Actividad 6] Permite importar `crearServidor` en pruebas automatizadas sin abrir puertos; solo inicia el listener si se ejecuta directamente con `node server.mjs`.
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  // [Actividad 6] Busca un argumento opcional `--port=<numero>` en la línea de comandos.
  const argPuerto = process.argv.find((arg) => arg.startsWith('--port='));
  // [Actividad 6] Busca un argumento opcional `--dir=<carpeta>` (por ejemplo `--dir=dist` en `npm run preview`).
  const argCarpeta = process.argv.find((arg) => arg.startsWith('--dir='));
  // [Actividad 6] Determina el puerto final priorizando `--port`, luego variable `PORT`, y por defecto `5173`.
  const puerto = Number(argPuerto?.split('=')[1] || process.env.PORT || 5173);
  // [Actividad 6] Define la interfaz de escucha priorizando `HOST` o `127.0.0.1` por seguridad local.
  const host = process.env.HOST || '127.0.0.1';
  // [Actividad 6] Resuelve la carpeta estática a servir (`public` por defecto o el valor de `--dir`).
  const carpeta = resolve(ROOT, argCarpeta?.split('=')[1] || 'public');
  // [Actividad 6] Instancia el servidor y lo pone en escucha en el host y puerto configurados.
  crearServidor({ directorio: carpeta }).listen(puerto, host, () => {
    // [Actividad 6] Muestra la URL local desde la cual el usuario abre la PWA AutoUTA.
    console.log(`PWA disponible: http://localhost:${puerto}`);
    // [Actividad 6] Muestra la dirección del Gateway NGINX SOA hacia donde se proxifican `/api/` y `/oauth/`.
    console.log(`Gateway SOA NGINX: ${process.env.API_BASE_URL || 'http://127.0.0.1:8090'}`);
    // [Actividad 6] Indica el atajo de teclado para detener el servidor local limpiamente.
    console.log('Para cerrar: Ctrl + C');
  });
}
