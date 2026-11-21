/**
 * Cliente HTTP para la consulta de ventas en React (APE 04).
 * Responsabilidad exclusiva: consulta de ventas y obtención de métricas.
 * Este cliente web NO contiene operaciones de creación/mutación de ventas.
 */
function normalizarToken(token = '') {
  return String(token).trim().replace(/^Bearer\s+/i, '').trim();
}

export async function listarVentas(token) {
  const headers = new Headers({ Accept: 'application/json' });
  const jwt = normalizarToken(token);
  if (jwt) {
    headers.set('Authorization', `Bearer ${jwt}`);
  }

  let respuesta;
  try {
    respuesta = await fetch('/api/ventas', {
      method: 'GET',
      headers,
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio de ventas a través de NGINX.');
  }

  if (!respuesta.ok) {
    const mensajes = {
      401: 'HTTP 401: Falta el JWT o el token ha expirado.',
      403: 'HTTP 403: El usuario no cuenta con permisos para auditar ventas.',
      404: 'HTTP 404: No se encontró el recurso /api/ventas.',
      500: 'HTTP 500: Error interno en el servicio de ventas (FastAPI).',
      502: 'HTTP 502: NGINX no pudo contactar a FastAPI (puerto 8002).',
      503: 'HTTP 503: El servicio de ventas no está disponible.',
      504: 'HTTP 504: Tiempo de espera agotado al consultar ventas.',
    };
    const error = new Error(
      mensajes[respuesta.status] ?? `HTTP ${respuesta.status}: Error al consultar el registro de ventas.`,
    );
    error.status = respuesta.status;
    throw error;
  }

  if (respuesta.status === 204) {
    return { ventas: [], codigoHttp: 204 };
  }

  let datos;
  try {
    datos = await respuesta.json();
  } catch {
    throw new Error('La respuesta de ventas no contiene JSON válido.');
  }

  if (!Array.isArray(datos)) {
    throw new Error('Se esperaba un arreglo JSON con el listado de ventas.');
  }

  return { ventas: datos, codigoHttp: respuesta.status };
}
