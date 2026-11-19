/**
 * Cliente HTTP para la consulta de ventas en React (APE 04).
 * Responsabilidad exclusiva: consulta de ventas y obtención de métricas.
 * Este cliente web NO contiene operaciones de creación/mutación de ventas.
 */
export async function listarVentas(token) {
  const headers = new Headers({ Accept: 'application/json' });
  if (token && token.trim()) {
    headers.set('Authorization', `Bearer ${token.trim()}`);
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
    const error = new Error(`HTTP ${respuesta.status}: Error al consultar el registro de ventas.`);
    error.status = respuesta.status;
    throw error;
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
