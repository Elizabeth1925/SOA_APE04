/**
 * ============================================================================
 * Archivo: public/api.js
 * Propósito:
 *   Capa cliente HTTP pura (módulo ES nativo) de la Aplicación Móvil PWA
 *   "AutoUTA". Encapsula todas las llamadas REST same-origin hacia el servidor
 *   proxy de la PWA (`server.mjs`), el cual las reenvía al Gateway NGINX
 *   (`:8090`) para consumir OAuth 2.0 / Spring Boot (autenticación y catálogo
 *   de vehículos) y FastAPI `:8002` (compradores, emisión de ventas con
 *   `Idempotency-Key` y consulta de ventas por ID).
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Login OAuth 2.0 real (`POST /oauth/token` con `idapli=92`),
 *     normalización del JWT en memoria, consultas autenticadas con
 *     `Authorization: Bearer` y `cache: 'no-store'`, emisión exclusiva de
 *     ventas (`POST /api/ventas` con cabecera `Idempotency-Key`) y consulta
 *     posterior (`GET /api/ventas/{id}`).
 *   - [Actividad 10 - P27] Uso consistente de identificadores transversales
 *     compartidos con Windows Forms, React y JavaFX (`vehiculoId`,
 *     `compradorId`, `id` de venta y `operacionId`).
 *   - [Actividad 11 - P08/P10/P11/P12/P13/503/504] Manejo explícito de errores
 *     HTTP 401/403/404/409/502/503/504 y fallos de red sin fabricar ventas ni
 *     inventar listas, propagando respuestas 200 idempotentes (P10), 409 por
 *     conflicto de idempotencia (P11) y 409 por vehículo ya vendido (P12).
 * ============================================================================
 */

/**
 * [Actividad 6] Normaliza un token JWT ingresado o recibido: limpia espacios
 * en los extremos y elimina un eventual prefijo redundante `Bearer `.
 */
export function normalizarToken(entrada = '') {
  // [Actividad 6] Convierte el valor a cadena, recorta espacios, quita el prefijo "Bearer" insensible a mayúsculas y vuelve a recortar.
  return String(entrada).trim().replace(/^Bearer\s+/i, '').trim();
}

/**
 * [Actividad 6] Autentica credenciales reales contra el servidor OAuth 2.0
 * (`POST /oauth/token`) usando flujo `grant_type=password` e `idapli=92`
 * a través del proxy inverso hacia NGINX.
 */
export async function loginOAuth(usuario, contrasena, ejecutarFetch = fetch) {
  // [Actividad 6] Construye el cuerpo `application/x-www-form-urlencoded` requerido por el endpoint OAuth 2.0.
  const parametros = new URLSearchParams();
  // [Actividad 6] Especifica el tipo de concesión OAuth 2.0 por usuario y contraseña (`password`).
  parametros.set('grant_type', 'password');
  // [Actividad 6] Agrega el nombre de usuario saneado sin espacios alrededor.
  parametros.set('username', String(usuario || '').trim());
  // [Actividad 6] Agrega la contraseña proporcionada por el usuario en el formulario de login.
  parametros.set('password', String(contrasena || ''));
  // [Actividad 6] Identifica la aplicación móvil PWA dentro de la arquitectura SOA (`idapli=92`).
  parametros.set('idapli', '92');

  // [Actividad 6] Codifica en Base64 el identificador y secreto del cliente OAuth 2.0 de laboratorio para autenticación Basic.
  const credencialesCliente = btoa('vehiculos-cliente:cliente-local-aula-2026-key-');
  // [Actividad 6] Construye los encabezados HTTP estándar exigidos por `/oauth/token`.
  const encabezados = new Headers({
    'Accept': 'application/json',
    'Content-Type': 'application/x-www-form-urlencoded',
    'Authorization': `Basic ${credencialesCliente}`,
  });

  // [Actividad 6] Variable que recibirá el objeto `Response` de la solicitud de autenticación.
  let respuesta;
  try {
    // [Actividad 6] Envía la petición POST same-origin `/oauth/token` sin almacenar en caché (`no-store`).
    respuesta = await ejecutarFetch('/oauth/token', {
      method: 'POST',
      headers: encabezados,
      body: parametros.toString(),
      cache: 'no-store',
    });
  } catch {
    // [Actividad 11 - 503/504] Traduce un fallo de red o caída del gateway en un mensaje claro sin inventar sesión.
    throw new Error('No se pudo conectar con el servicio de autenticación. Verifique NGINX y Spring Boot.');
  }

  // [Actividad 6] Variable para almacenar el cuerpo JSON devuelto por el servidor de autorización.
  let datos;
  try {
    // [Actividad 6] Parsea la respuesta JSON (token emitido o detalle de error OAuth 2.0).
    datos = await respuesta.json();
  } catch {
    // [Actividad 11] Respalda con un objeto controlado si el backend devolvió texto plano o HTML en vez de JSON.
    datos = { error: 'Respuesta inválida del servidor' };
  }

  // [Actividad 11 - P08/503/504] Si el estado HTTP no es 2xx (p. ej. 400, 401, 403, 502, 503, 504), lanza error enriquecido.
  if (!respuesta.ok) {
    // [Actividad 11 - P08] Extrae la descripción más específica del rechazo devuelto por Spring Boot / OAuth 2.0.
    const detalle = datos.mensaje || datos.error_description || datos.error || 'Credenciales inválidas';
    // [Actividad 11 - P08] Construye el error incluyendo el código HTTP real para trazabilidad en pantalla.
    const error = new Error(`HTTP ${respuesta.status}: ${detalle}`);
    // [Actividad 11] Adjunta el código HTTP numérico para decisiones de UI.
    error.codigoHttp = respuesta.status;
    // [Actividad 11] Adjunta el payload completo del error devuelto por el servidor.
    error.detalles = datos;
    // [Actividad 11] Propaga la excepción al controlador de la interfaz.
    throw error;
  }

  // [Actividad 6] Retorna el token JWT y sus metadatos para ser guardados exclusivamente en memoria volátil.
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
 * [Actividad 6] [Actividad 10 - P27] Consulta el catálogo real de vehículos en
 * Spring Boot (`GET /api/vehiculos`) a través del proxy y NGINX, compartiendo
 * los mismos `idVehiculo` y estados que Windows Forms, React y JavaFX.
 */
export async function listarVehiculos(entradaToken, ejecutarFetch = fetch) {
  // [Actividad 6] Limpia el token JWT antes de incluirlo en la cabecera Authorization.
  const jwt = normalizarToken(entradaToken);
  // [Actividad 6] Solicita explícitamente representación JSON en la cabecera Accept.
  const encabezados = new Headers({ Accept: 'application/json' });

  // [Actividad 6] [Actividad 11 - P08] Solo agrega `Authorization: Bearer` si existe token; si falta, el backend decidirá HTTP 401.
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  // [Actividad 6] Variable que contendrá la respuesta HTTP de `/api/vehiculos`.
  let respuesta;
  try {
    // [Actividad 6] Ejecuta `GET /api/vehiculos` sin caché (`no-store`) para reflejar ventas recién ejecutadas en tiempo real.
    respuesta = await ejecutarFetch('/api/vehiculos', {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    // [Actividad 11 - 503/504] Notifica fallo de conectividad real sin devolver arreglos ficticios.
    throw new Error('No se pudo conectar con el servicio. Compruebe la red y Spring Boot.');
  }

  // [Actividad 11 - P08/503/504] Evalúa códigos de error HTTP del gateway o de Spring Boot.
  if (!respuesta.ok) {
    // [Actividad 11 - P08/503/504] Tabla de mensajes específicos para 401, 403, 404, 502, 503 y 504.
    const mensajes = {
      401: 'HTTP 401: JWT ausente, inválido o vencido.',
      403: 'HTTP 403: el usuario no tiene permiso para consultar.',
      404: 'HTTP 404: no se encontró /api/vehiculos.',
      502: 'HTTP 502: no se pudo establecer conexión con Spring Boot.',
      503: 'HTTP 503: el servicio no está disponible.',
      504: 'HTTP 504: Spring Boot no respondió a tiempo.',
    };
    // [Actividad 11] Crea el Error con el mensaje mapeado o un mensaje genérico conservando el código HTTP real.
    const error = new Error(mensajes[respuesta.status] || `HTTP ${respuesta.status}: consulta no completada.`);
    // [Actividad 11] Expone el código HTTP en el objeto Error para inspección en UI y pruebas unitarias.
    error.codigoHttp = respuesta.status;
    // [Actividad 11] Lanza el error para impedir que se pinte un inventario inexistente.
    throw error;
  }

  // [Actividad 6] Variable que recibirá el JSON deserializado del inventario.
  let datos;
  try {
    // [Actividad 6] Parsea el cuerpo de la respuesta como JSON.
    datos = await respuesta.json();
  } catch {
    // [Actividad 11] Rechaza respuestas corruptas o no-JSON para mantener integridad contractual.
    throw new Error('La API devolvió una respuesta que no es JSON válido.');
  }
  // [Actividad 6] [Actividad 11] Valida el contrato REST: `/api/vehiculos` debe devolver obligatoriamente un arreglo JSON.
  if (!Array.isArray(datos)) {
    throw new Error('La API debe devolver un arreglo JSON de vehículos.');
  }
  // [Actividad 6] [Actividad 10 - P27] Retorna el estado HTTP y la lista real de vehículos sincronizada con el resto de clientes SOA.
  return { codigoHttp: respuesta.status, vehiculos: datos };
}

/**
 * [Actividad 6] [Actividad 10 - P27] Consulta la lista de compradores
 * registrados en FastAPI (`GET /api/compradores`) a través de NGINX para
 * asociar `compradorId` válidos en la emisión de ventas.
 */
export async function listarCompradores(entradaToken, ejecutarFetch = fetch) {
  // [Actividad 6] Normaliza el token Bearer en memoria antes de enviarlo.
  const jwt = normalizarToken(entradaToken);
  // [Actividad 6] Declara cabecera Accept para recibir respuesta JSON.
  const encabezados = new Headers({ Accept: 'application/json' });
  // [Actividad 6] Adjunta el JWT en `Authorization` cuando la sesión posee un token válido.
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  // [Actividad 6] Variable para la respuesta HTTP del microservicio de compradores (FastAPI).
  let respuesta;
  try {
    // [Actividad 6] Ejecuta `GET /api/compradores` en modo `no-store` a través del proxy hacia NGINX.
    respuesta = await ejecutarFetch('/api/compradores', {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    // [Actividad 11 - 503/504] Informa error de conectividad hacia el servicio de compradores (FastAPI).
    throw new Error('No se pudo conectar con el servicio de compradores (FastAPI).');
  }

  // [Actividad 11 - P08/503/504] Maneja respuestas HTTP no exitosas sin fabricar compradores ficticios.
  if (!respuesta.ok) {
    // [Actividad 11 - P08/503/504] Mensajes descriptivos por código HTTP (401, 403, 404, 502, 503, 504).
    const mensajes = {
      401: 'HTTP 401: JWT ausente, inválido o vencido.',
      403: 'HTTP 403: el usuario no tiene permiso para consultar compradores.',
      404: 'HTTP 404: no se encontró /api/compradores.',
      502: 'HTTP 502: FastAPI (puerto 8002) no responde tras NGINX.',
      503: 'HTTP 503: servicio de compradores no disponible.',
      504: 'HTTP 504: tiempo de espera agotado al consultar compradores.',
    };
    // [Actividad 11] Instancia el Error con el detalle correspondiente al estado HTTP recibido.
    const error = new Error(mensajes[respuesta.status] || `HTTP ${respuesta.status}: Error al consultar compradores.`);
    // [Actividad 11] Guarda el código HTTP en el error para que el selector de compradores pueda mostrarlo.
    error.codigoHttp = respuesta.status;
    // [Actividad 11] Propaga el fallo al llamador.
    throw error;
  }

  // [Actividad 6] Parsea el arreglo JSON de compradores recibido desde FastAPI.
  const datos = await respuesta.json();
  // [Actividad 6] [Actividad 10 - P27] Devuelve el código HTTP y la lista de compradores (o arreglo vacío si no es array).
  return { codigoHttp: respuesta.status, compradores: Array.isArray(datos) ? datos : [] };
}

/**
 * [Actividad 6] [Actividad 10 - P27] [Actividad 11 - P08/P10/P11/P12/P13/503/504]
 * Emite una venta comercial real (`POST /api/ventas`) con la cabecera obligatoria
 * `Idempotency-Key`. La PWA es el único canal autorizado en la arquitectura SOA
 * para emitir ventas; soporta creación (HTTP 201), reintento idempotente (HTTP 200,
 * P10) y rechazo real de conflictos (HTTP 409, P11/P12) o falta de permisos (401/403).
 */
export async function registrarVenta(datosVenta, idempotencyKey, entradaToken, ejecutarFetch = fetch) {
  // [Actividad 6] Normaliza el JWT almacenado en memoria volátil.
  const jwt = normalizarToken(entradaToken);
  // [Actividad 6] Define cabeceras JSON para enviar y recibir el contrato de venta.
  const encabezados = new Headers({
    'Accept': 'application/json',
    'Content-Type': 'application/json',
  });

  // [Actividad 6] Agrega el JWT Bearer para que FastAPI valide identidad y rol (`OPERADOR` o `ADMIN`).
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);
  // [Actividad 6] [Actividad 11 - P10/P11] Inyecta la cabecera `Idempotency-Key` para garantizar control exacto de reintentos y duplicados.
  if (idempotencyKey) encabezados.set('Idempotency-Key', String(idempotencyKey).trim());

  // [Actividad 6] Variable para la respuesta HTTP de `POST /api/ventas`.
  let respuesta;
  try {
    // [Actividad 6] [Actividad 10 - P27] Envía el payload `{ vehiculoId, compradorId, precio }` hacia `/api/ventas` vía NGINX sin caché.
    respuesta = await ejecutarFetch('/api/ventas', {
      method: 'POST',
      headers: encabezados,
      body: JSON.stringify(datosVenta),
      cache: 'no-store',
    });
  } catch {
    // [Actividad 11 - 503/504] Lanza error de conectividad sin fingir ni confirmar una venta local.
    throw new Error('No se pudo establecer conexión con el servicio de ventas.');
  }

  // [Actividad 6] Variable que recibirá el cuerpo JSON (comprobante de venta o Problem Details / error de negocio).
  let datos;
  try {
    // [Actividad 6] Parsea la respuesta JSON devuelta por FastAPI.
    datos = await respuesta.json();
  } catch {
    // [Actividad 11] Protege contra respuestas no JSON (p. ej. página de error de gateway).
    datos = { mensaje: 'Respuesta no parseable como JSON' };
  }

  // [Actividad 11 - P08/P11/P12/P13/503/504] Si la venta fue rechazada (401, 403, 404, 409, 502, 503, 504), propaga error estructurado.
  if (!respuesta.ok) {
    // [Actividad 11] Extrae el detalle de error enviado por FastAPI (`detail`, `mensaje`, `message` o `title` RFC 7807).
    const detalleServidor = datos.detail || datos.mensaje || datos.message || datos.title;
    // [Actividad 11 - P08/P11/P12/503] Mensajes de respaldo por código HTTP cuando el servidor no incluya texto descriptivo.
    const mensajesPorDefecto = {
      401: 'No autorizado. Inicie sesión nuevamente.',
      403: 'Permisos insuficientes para registrar ventas (requiere rol OPERADOR o ADMIN).',
      404: 'Vehículo o comprador no encontrado.',
      409: 'Conflicto de negocio o clave de idempotencia reutilizada con datos distintos.',
      502: 'Servicio de ventas (FastAPI :8002) no disponible a través de NGINX.',
      503: 'Servicio de ventas temporalmente no disponible.',
    };
    // [Actividad 11] Compone el mensaje final anteponiendo el código HTTP real devuelto por el backend.
    const mensajeError = `HTTP ${respuesta.status}: ${detalleServidor || mensajesPorDefecto[respuesta.status] || 'Registro de venta rechazado.'}`;
    // [Actividad 11] Construye el objeto Error con código y detalles técnicos para mostrar en el comprobante de error.
    const error = new Error(mensajeError);
    // [Actividad 11] Adjunta el código HTTP numérico (p. ej. 409 para distinguir conflicto de otros errores).
    error.codigoHttp = respuesta.status;
    // [Actividad 11] Adjunta el JSON de error del backend (`codigo`, `detail`, `traceId`) para auditoría y UI.
    error.detalles = datos;
    // [Actividad 11] Propaga el error sin registrar ni inventar ninguna venta.
    throw error;
  }

  // [Actividad 6] [Actividad 10 - P27] [Actividad 11 - P10] Devuelve código HTTP (201 nueva venta o 200 idempotente) y el objeto `venta` (`id`, `vehiculoId`, `compradorId`, `operacionId`).
  return { codigoHttp: respuesta.status, venta: datos };
}

/**
 * [Actividad 6] [Actividad 10 - P27] Consulta el estado persistido y detalle
 * de una venta emitida por su identificador (`GET /api/ventas/{id}`),
 * permitiendo verificar `ventaId`, `vehiculoId`, `compradorId`, `operacionId`
 * y estado de notificación RabbitMQ.
 */
export async function consultarVenta(ventaId, entradaToken, ejecutarFetch = fetch) {
  // [Actividad 6] Normaliza el JWT vigente en memoria.
  const jwt = normalizarToken(entradaToken);
  // [Actividad 6] Prepara la cabecera Accept para recibir JSON.
  const encabezados = new Headers({ Accept: 'application/json' });
  // [Actividad 6] Adjunta `Authorization: Bearer` si hay token disponible.
  if (jwt) encabezados.set('Authorization', `Bearer ${jwt}`);

  // [Actividad 6] Variable para almacenar la respuesta de `GET /api/ventas/{id}`.
  let respuesta;
  try {
    // [Actividad 6] [Actividad 10 - P27] Consulta `/api/ventas/{id}` codificando de forma segura el identificador en la URL y sin caché.
    respuesta = await ejecutarFetch(`/api/ventas/${encodeURIComponent(ventaId)}`, {
      method: 'GET',
      headers: encabezados,
      cache: 'no-store',
    });
  } catch {
    // [Actividad 11 - 503/504] Informa fallo de red al intentar consultar la venta.
    throw new Error('No se pudo conectar con el servicio de ventas.');
  }

  // [Actividad 6] Variable que almacenará el JSON de la venta consultada o del error.
  let datos;
  try {
    // [Actividad 6] Deserializa la respuesta JSON del servicio de ventas.
    datos = await respuesta.json();
  } catch {
    // [Actividad 11] Si la respuesta no es JSON válido, establece `null` de forma controlada.
    datos = null;
  }

  // [Actividad 11 - P08/P13/503/504] Si la consulta falla (p. ej. 401, 403, 404 venta inexistente, 503), lanza un error con código HTTP real.
  if (!respuesta.ok) {
    // [Actividad 11] Construye el Error priorizando `datos.detail` del backend o un mensaje con el código HTTP.
    const error = new Error(datos?.detail || `HTTP ${respuesta.status}: Venta no encontrada.`);
    // [Actividad 11] Adjunta el código HTTP para renderizar el comprobante de error correctamente.
    error.codigoHttp = respuesta.status;
    // [Actividad 11] Propaga el error al manejador de consulta en `main.js`.
    throw error;
  }

  // [Actividad 6] [Actividad 10 - P27] Devuelve el código HTTP y el registro completo de la venta consultada.
  return { codigoHttp: respuesta.status, venta: datos };
}
