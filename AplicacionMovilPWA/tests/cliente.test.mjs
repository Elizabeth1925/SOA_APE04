/**
 * ============================================================================
 * Archivo: tests/cliente.test.mjs
 * Propósito:
 *   Suite de pruebas unitarias automatizadas (6 casos de prueba con `node:test`)
 *   que verifica el comportamiento del cliente HTTP de la PWA (`public/api.js`)
 *   mediante dobles de prueba (`simularFetch`) sin requerir servicios externos:
 *   normalización del token Bearer, envío correcto de `Authorization` y
 *   `cache: 'no-store'`, ausencia de cabecera cuando no hay token, detección
 *   estricta de HTTP 401, validación del contrato JSON (arreglo) y manejo de
 *   fallos de red.
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Verificación del cliente HTTP OAuth 2.0 / JWT de la PWA
 *     (`normalizarToken`, `listarVehiculos`, cabecera `Authorization: Bearer`,
 *     política `cache: 'no-store'`).
 *   - [Actividad 10 - P27] Verificación del contrato de datos compartido con
 *     otros clientes (`idVehiculo` dentro del arreglo JSON del inventario).
 *   - [Actividad 11 - P08/503/504] Comprobación de rechazo real ante HTTP 401
 *     sin fabricar listas ficticias, validación de payload malformado y
 *     notificación controlada ante pérdida de conectividad.
 * ============================================================================
 */

// [Actividad 6] Importa el corredor de pruebas nativo de Node.js (`node:test`).
import test from 'node:test';
// [Actividad 6] Importa aserciones estrictas de Node.js para validar igualdad, errores y rechazos de promesas.
import assert from 'node:assert/strict';
// [Actividad 6] Importa las funciones del cliente HTTP de la PWA bajo prueba desde `public/api.js`.
import { listarVehiculos, normalizarToken } from '../public/api.js';

// [Actividad 6] Aclaración metodológica: estas pruebas usan respuestas simuladas y complementan (no reemplazan) la integración real vía NGINX.
// Los datos de estas pruebas son simulados y no sustituyen a la API real.

// [Actividad 6] Caso 1: comprueba que `normalizarToken` elimine espacios sobrantes y el prefijo redundante `Bearer` sin importar mayúsculas/minúsculas.
test('normaliza un token pegado con prefijo Bearer', () => {
  // [Actividad 6] Verifica que la cadena saneada conserve únicamente el JWT puro (de prueba).
  assert.equal(normalizarToken('  bearer abc.def.ghi  '), 'abc.def.ghi');
});

// [Actividad 6] [Actividad 10 - P27] Caso 2: comprueba que `GET /api/vehiculos` envíe `Authorization: Bearer`, desactive caché y devuelva el arreglo JSON.
test('GET transmite Authorization y recibe una lista JSON', async () => {
  // [Actividad 6] Variable auxiliar para inspeccionar la ruta y opciones con las que se invoca `fetch`.
  let solicitud;
  // [Actividad 6] Doble de prueba que captura la llamada HTTP y responde con un arreglo JSON válido con `idVehiculo`.
  const simularFetch = async (ruta, opciones) => {
    // [Actividad 6] Registra los argumentos exactos recibidos por `ejecutarFetch`.
    solicitud = { ruta, opciones };
    // [Actividad 10 - P27] Retorna una respuesta HTTP 200 con un vehículo de ejemplo usando el campo contractual `idVehiculo`.
    return new Response(JSON.stringify([{ idVehiculo: 1, marca: 'Ejemplo' }]), {
      status: 200, headers: { 'Content-Type': 'application/json' },
    });
  };
  // [Actividad 6] Ejecuta `listarVehiculos` inyectando el token simulado y la función `simularFetch`.
  const resultado = await listarVehiculos('mi.token', simularFetch);
  // [Actividad 6] Confirma que la petición apunta al endpoint same-origin `/api/vehiculos` (enrutado por proxy hacia NGINX).
  assert.equal(solicitud.ruta, '/api/vehiculos');
  // [Actividad 6] Confirma que el token se inyectó con el esquema estándar `Bearer <token>`.
  assert.equal(solicitud.opciones.headers.get('Authorization'), 'Bearer mi.token');
  // [Actividad 6] Confirma que se solicita `cache: 'no-store'` para no persistir datos protegidos en caché.
  assert.equal(solicitud.opciones.cache, 'no-store');
  // [Actividad 6] Verifica que el resultado parseado contiene exactamente el registro esperado.
  assert.equal(resultado.vehiculos.length, 1);
});

// [Actividad 6] [Actividad 11 - P08] Caso 3: comprueba que sin token no se envíe `Authorization` malformado, delegando la decisión HTTP 401 al backend.
test('sin token no agrega Authorization (el backend decide 401)', async () => {
  // [Actividad 11 - P08] Simula `fetch` verificando explícitamente la ausencia de la cabecera `Authorization`.
  const simularFetch = async (_ruta, opciones) => {
    // [Actividad 11 - P08] Aserta que no existe cabecera `Authorization` cuando el token está vacío.
    assert.equal(opciones.headers.has('Authorization'), false);
    // [Actividad 6] Devuelve un arreglo vacío HTTP 200 para completar el flujo de la prueba unitaria.
    return new Response('[]', { status: 200 });
  };
  // [Actividad 6] Invoca `listarVehiculos` con cadena vacía como token.
  await listarVehiculos('', simularFetch);
});

// [Actividad 11 - P08] Caso 4: verifica que ante HTTP 401 el cliente rechace la promesa con `codigoHttp === 401` sin inventar datos.
test('identifica HTTP 401 sin mostrar una lista inventada', async () => {
  // [Actividad 11 - P08] Comprueba que `listarVehiculos` lanza un Error enriquecido con código 401 y mensaje explícito.
  await assert.rejects(
    listarVehiculos('vencido', async () => new Response('No autorizado', { status: 401 })),
    (error) => error.codigoHttp === 401 && /401/.test(error.message),
  );
});

// [Actividad 6] [Actividad 11] Caso 5: verifica el cumplimiento del contrato JSON rechazando respuestas que no sean un arreglo.
test('rechaza un objeto en lugar del arreglo JSON contratado', async () => {
  // [Actividad 11] Simula una respuesta 200 con objeto `{ data: [] }` en vez de un arreglo y exige rechazo con mensaje sobre arreglo JSON.
  await assert.rejects(
    listarVehiculos('jwt', async () => new Response('{"data":[]}', { status: 200 })),
    /arreglo JSON/,
  );
});

// [Actividad 11 - 503/504] Caso 6: verifica que una caída de red o indisponibilidad del servicio informe un error claro de conectividad.
test('informa error de conectividad', async () => {
  // [Actividad 11 - 503/504] Simula una excepción de red en `fetch` y verifica que el mensaje resultante informe imposibilidad de conectar.
  await assert.rejects(
    listarVehiculos('jwt', async () => { throw new Error('sin red'); }),
    /conectar/,
  );
});
