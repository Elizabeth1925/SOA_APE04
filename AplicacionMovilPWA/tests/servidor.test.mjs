/**
 * ============================================================================
 * Archivo: tests/servidor.test.mjs
 * Propósito:
 *   Prueba de integración local automatizada (1 caso de prueba con `node:test`)
 *   que levanta en puertos efímeros un servidor backend simulado y el servidor
 *   proxy de la PWA (`server.mjs`), verificando el reenvío transparente de la
 *   ruta `/api/vehiculos`, la preservación de la cabecera `Authorization: Bearer`,
 *   la política `Cache-Control: no-store` y el cuerpo JSON devuelto.
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Validación del proxy inverso same-origin en `server.mjs`
 *     hacia el gateway (`API_BASE_URL` / NGINX `:8090`), asegurando propagación
 *     de JWT en cabecera `Authorization` y prohibición de caché (`no-store`).
 * ============================================================================
 */

// [Actividad 6] Importa el corredor de pruebas nativo de Node.js.
import test from 'node:test';
// [Actividad 6] Importa el módulo de aserciones estrictas.
import assert from 'node:assert/strict';
// [Actividad 6] Importa `createServer` para levantar un backend HTTP simulado efímero en localhost.
import { createServer } from 'node:http';
// [Actividad 6] Importa `once` para esperar de forma asíncrona el evento `listening` de cada servidor.
import { once } from 'node:events';
// [Actividad 6] Importa la fábrica del servidor PWA/proxy inverso definida en `server.mjs`.
import { crearServidor } from '../server.mjs';

// [Actividad 6] Prueba de integración local: levanta un backend y el proxy PWA en puertos aleatorios (`0`) para no interferir con puertos fijos.
// Prueba de integración local: dos servidores de prueba en puertos aleatorios.
test('proxy reenvía Bearer y respuesta HTTP del backend', async () => {
  // [Actividad 6] Variable para capturar la cabecera `Authorization` que efectivamente llega al servidor destino.
  let jwt = '';
  // [Actividad 6] Crea un servidor HTTP temporal que simula al Gateway NGINX / backend respondiendo `[]`.
  const backend = createServer((req, res) => {
    // [Actividad 6] Guarda el valor exacto de la cabecera `authorization` reenviada por el proxy.
    jwt = req.headers.authorization;
    // [Actividad 6] Responde HTTP 200 con tipo de contenido JSON.
    res.writeHead(200, { 'Content-Type': 'application/json' });
    // [Actividad 6] Envía un arreglo JSON vacío como respuesta contractual.
    res.end('[]');
  });
  // [Actividad 6] Inicia el backend simulado en un puerto libre de `127.0.0.1` y espera a que esté escuchando.
  backend.listen(0, '127.0.0.1'); await once(backend, 'listening');
  // [Actividad 6] Obtiene el puerto aleatorio asignado por el sistema operativo al backend simulado.
  const port = backend.address().port;
  // [Actividad 6] Crea una instancia del servidor PWA apuntando su `apiBase` al backend simulado.
  const frontend = crearServidor({ apiBase: `http://127.0.0.1:${port}` });
  // [Actividad 6] Inicia el servidor PWA en otro puerto aleatorio local y espera a que quede listo.
  frontend.listen(0, '127.0.0.1'); await once(frontend, 'listening');
  try {
    // [Actividad 6] Realiza una petición GET real contra `/api/vehiculos` del servidor PWA con un Bearer token de prueba.
    const respuesta = await fetch(`http://127.0.0.1:${frontend.address().port}/api/vehiculos`, {
      headers: { Authorization: 'Bearer prueba.jwt' },
    });
    // [Actividad 6] Verifica que el proxy preservó el código de estado HTTP 200 del backend.
    assert.equal(respuesta.status, 200);
    // [Actividad 6] Verifica que la cabecera `Authorization: Bearer prueba.jwt` llegó intacta al backend.
    assert.equal(jwt, 'Bearer prueba.jwt');
    // [Actividad 6] Verifica que el proxy impone `Cache-Control: no-store` en las respuestas de API.
    assert.equal(respuesta.headers.get('cache-control'), 'no-store');
    // [Actividad 6] Verifica que el cuerpo JSON recibido coincide con el arreglo vacío devuelto por el backend.
    assert.deepEqual(await respuesta.json(), []);
  } finally {
    // [Actividad 6] Cierra ordenadamente el servidor frontend PWA al concluir la prueba para no dejar procesos abiertos.
    await new Promise((r) => frontend.close(r));
    // [Actividad 6] Cierra ordenadamente el servidor backend simulado para liberar el puerto efímero.
    await new Promise((r) => backend.close(r));
  }
});
