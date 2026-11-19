/**
 * Cliente HTTP para la PWA Móvil de Vehículos y Ventas (SOA APE 04).
 * Gestiona autenticación OAuth 2.0 / JWT, consulta de vehículos y compradores,
 * e interactúa con la API de ventas mediante Idempotency-Key a través de NGINX.
 */
export function normalizarToken(entrada = '') {
  return String(entrada).trim().replace(/^Bearer\s+/i, '').trim();
}

/**
 * Autentica credenciales contra el servidor OAuth 2.0 (/oauth/token).
 */
export async function loginOAuth(usuario, contrasena, ejecutarFetch = fetch) {
  const parametros = new URLSearchParams();
  parametros.set('grant_type', 'password');
  parametros.set('username', String(usuario || '').trim());
  parametros.set('password', String(contrasena || ''));

  const credencialesCliente = btoa('vehiculos-cliente:cliente-local-aula-2026-key-');
  const encabezados = new Headers({
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Authorization': `Basic ${credencialesCliente}`,
  });

  let respuesta;
  try {
    respuesta = await ejecutarFetch('/oauth/token', {
      method: 'POST',
      headers: encabezados,
      body: parametros.toString(),
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio de autenticación. Verifique NGINX y Spring Boot.');
  }

  let datos;
  try {
    datos = await respuesta.json();
  } catch {
    datos = { error: 'Respuesta inválida del servidor' };
  }

  if (!respuesta.ok) {
    const detalle = datos.error_description || datos.error || 'Credenciales inválidas';
    const error = new Error(`HTTP ${respuesta.status}: ${detalle}`);
    error.codigoHttp = respuesta.status;
    error.detalles = datos;
    throw error;
  }

  return {
    codigoHttp: respuesta.status,
    token: datos.access_token,
    tokenTipo: datos.token_type,
    expiraEn: datos.expires_in,
    scope: datos.scope,
    jti: datos.jti,
  };
}

/**
 * Consulta el catálogo de vehículos en Spring Boot a través de NGINX.
 */
export async function listarVehiculos(entradaToken, ejecutarFetch = fetch) {
  const jwt = normalizarToken(entradaToken);
  const encabezados = new Headers({ Accept: 'application/json' });

  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  let respuesta;
  try {
    respuesta = await ejecutarFetch('/api/vehiculos', {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio. Compruebe la red y Spring Boot.');
  }

  if (!respuesta.ok) {
    const mensajes = {
      401: 'HTTP 401: JWT ausente, inválido o vencido.',
      403: 'HTTP 403: el usuario no tiene permiso para consultar.',
      404: 'HTTP 404: no se encontró /api/vehiculos.',
      502: 'HTTP 502: no se pudo establecer conexión con Spring Boot.',
      503: 'HTTP 503: el servicio no está disponible.',
      504: 'HTTP 504: Spring Boot no respondió a tiempo.',
    };
    const error = new Error(mensajes[respuesta.status] || `HTTP ${respuesta.status}: consulta no completada.`);
    error.codigoHttp = respuesta.status;
    throw error;
  }

  let datos;
  try {
    datos = await respuesta.json();
  } catch {
    throw new Error('La API devolvió una respuesta que no es JSON válido.');
  }
  if (!Array.isArray(datos)) {
    throw new Error('La API debe devolver un arreglo JSON de vehículos.');
  }
  return { codigoHttp: respuesta.status, vehiculos: datos };
}

/**
 * Consulta la lista de compradores registrados en FastAPI a través de NGINX.
 */
export async function listarCompradores(entradaToken, ejecutarFetch = fetch) {
  const jwt = normalizarToken(entradaToken);
  const encabezados = new Headers({ Accept: 'application/json' });
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  let respuesta;
  try {
    respuesta = await ejecutarFetch('/api/compradores', {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio de compradores (FastAPI).');
  }

  if (!respuesta.ok) {
    const error = new Error(`HTTP ${respuesta.status}: Error al consultar compradores.`);
    error.codigoHttp = respuesta.status;
    throw error;
  }

  const datos = await respuesta.json();
  return { codigoHttp: respuesta.status, compradores: Array.isArray(datos) ? datos : [] };
}

/**
 * Emite una venta comercial (POST /api/ventas) con cabecera Idempotency-Key.
 * Exclusivo de la aplicación móvil PWA en la arquitectura SOA.
 */
export async function registrarVenta(datosVenta, idempotencyKey, entradaToken, ejecutarFetch = fetch) {
  const jwt = normalizarToken(entradaToken);
  const encabezados = new Headers({
    'Accept': 'application/json',
    'Content-Type': 'application/json',
  });

  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);
  if (idempotencyKey) encabezados.set('Idempotency-Key', String(idempotencyKey).trim());

  let respuesta;
  try {
    respuesta = await ejecutarFetch('/api/ventas', {
      method: 'POST',
      headers: encabezados,
      body: JSON.stringify(datosVenta),
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo establecer conexión con el servicio de ventas.');
  }

  let datos;
  try {
    datos = await respuesta.json();
  } catch {
    datos = { mensaje: 'Respuesta no parseable como JSON' };
  }

  if (!respuesta.ok) {
    const mensajeError = datos.detail || datos.message || `HTTP ${respuesta.status}: Registro de venta rechazado.`;
    const error = new Error(mensajeError);
    error.codigoHttp = respuesta.status;
    error.detalles = datos;
    throw error;
  }

  return { codigoHttp: respuesta.status, venta: datos };
}

/**
 * Consulta el estado y detalle de una venta por su ID.
 */
export async function consultarVenta(ventaId, entradaToken, ejecutarFetch = fetch) {
  const jwt = normalizarToken(entradaToken);
  const encabezados = new Headers({ Accept: 'application/json' });
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  let respuesta;
  try {
    respuesta = await ejecutarFetch(`/api/ventas/${encodeURIComponent(ventaId)}`, {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    throw new Error('No se pudo conectar con el servicio de ventas.');
  }

  let datos;
  try {
    datos = await respuesta.json();
  } catch {
    datos = null;
  }

  if (!respuesta.ok) {
    const error = new Error(datos?.detail || `HTTP ${respuesta.status}: Venta no encontrada.`);
    error.codigoHttp = respuesta.status;
    throw error;
  }

  return { codigoHttp: respuesta.status, venta: datos };
}
