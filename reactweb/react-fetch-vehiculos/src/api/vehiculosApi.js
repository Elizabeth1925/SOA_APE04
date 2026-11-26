/*
 * ============================================================================
 * Archivo: src/api/vehiculosApi.js
 * Proyecto: Portal Web React de Consulta y Auditoría (react-fetch-vehiculos) - SOA APE 04
 * Función:
 *   Cliente HTTP dedicado en modo solo lectura para consultar el catálogo de
 *   vehículos (GET /api/vehiculos y GET /api/vehiculos?marca=...) usando Fetch
 *   API a través del proxy de Vite hacia el Gateway NGINX (:8090).
 * Actividades APE 04 cubiertas:
 *   - [Actividad 7]: Consumo de GET /api/vehiculos con soporte de filtrado
 *     remoto por marca y manejo de listado vacío (incluyendo HTTP 204).
 *   - [Actividad 10 - P27]: Uso de cache: 'no-store' para garantizar lectura
 *     fresca del estado real de cada vehículo (DISPONIBLE -> VENDIDO) sin datos
 *     obsoletos en caché del navegador.
 *   - [Actividad 11]: Envío condicional del JWT (omitirlo permite validar
 *     HTTP 401 real del backend) y manejo controlado de códigos 401/403/502/504
 *     sin inventar ni simular registros de catálogo.
 * ============================================================================
 */
/**
 * Cliente HTTP de la API de vehículos.
 *
 * Responsabilidad: enviar GET, agregar JWT e interpretar HTTP/JSON.
 * No contiene código visual, consultas SQL ni reglas de negocio.
 *
 * La ruta es relativa porque el proxy de Vite (/api) conecta con NGINX (:8090) y Java.
 */
// Limpia el token JWT recibido eliminando espacios laterales y un posible prefijo duplicado "Bearer ".
function normalizarToken(token = '') {
  // Convierte a cadena, recorta espacios, quita "Bearer " sin distinguir mayúsculas y vuelve a recortar.
  return String(token).trim().replace(/^Bearer\s+/i, '').trim();
}

// Ejecuta la petición autenticada GET /api/vehiculos (opcionalmente filtrada por marca) vía proxy Vite -> NGINX [Actividad 7 / Actividad 10 - P27 / Actividad 11].
export async function listarVehiculos(token, marca = '') {
  // Headers es el objeto estándar del navegador para construir cabeceras HTTP; solicita respuesta JSON.
  const headers = new Headers({ Accept: 'application/json' });

  // Solo se envía Authorization si existe un token escrito por el usuario.
  // Dejarlo vacío permite comprobar el rechazo HTTP 401 real del backend sin inventar datos [Actividad 11].
  const jwt = normalizarToken(token);
  // Si el token normalizado no está vacío, adjunta el encabezado estándar Authorization Bearer.
  if (jwt) {
    headers.set('Authorization', `Bearer ${jwt}`);
  }

  // Normaliza el parámetro opcional de marca eliminando espacios sobrantes [Actividad 7].
  const filtro = String(marca || '').trim();
  // Construye la ruta relativa (/api/vehiculos o /api/vehiculos?marca=...) codificando caracteres especiales para el proxy de Vite [Actividad 7].
  const ruta = filtro ? `/api/vehiculos?marca=${encodeURIComponent(filtro)}` : '/api/vehiculos';

  // Variable que almacenará el objeto Response devuelto por fetch.
  let respuesta;
  try {
    // fetch solicita recursos de forma asíncrona sin bloquear la interfaz de React [Actividad 7].
    respuesta = await fetch(ruta, {
      // Método estrictamente de consulta (solo lectura).
      method: 'GET',
      // Cabeceras Accept y Authorization (si existe JWT).
      headers,
      // Desactiva caché HTTP para reflejar inmediatamente transiciones DISPONIBLE -> VENDIDO [Actividad 10 - P27].
      cache: 'no-store', // Evita mostrar un catálogo almacenado en caché.
    });
  } catch {
    // Un fallo de red impide recibir una respuesta HTTP; notifica verificar NGINX (:8090) [Actividad 11].
    throw new Error('No se pudo conectar. Verifique el Gateway NGINX (:8090) y la red.');
  }

  // fetch NO lanza una excepción automática por códigos como 401 o 403.
  // Por eso, se debe verificar manualmente respuesta.ok y traducir cada código HTTP real [Actividad 11].
  if (!respuesta.ok) {
    // Diccionario de mensajes claros y controlados por código de error HTTP [Actividad 11].
    const mensajes = {
      // 401: sin token o token inválido/expirado [Actividad 11].
      401: 'HTTP 401: Falta el JWT o el token ya no es válido.',
      // 403: token válido pero sin permisos/roles requeridos [Actividad 11].
      403: 'HTTP 403: El usuario no tiene permiso para consultar.',
      // 404: endpoint no encontrado en NGINX/backend.
      404: 'HTTP 404: No existe la ruta /api/vehiculos.',
      // 500: fallo interno en la API Java de vehículos.
      500: 'HTTP 500: Error del servidor o de la conexión con la API Java.',
      // 502: NGINX no logra comunicarse con ninguna instancia upstream de vehículos [Actividad 11].
      502: 'HTTP 502: NGINX no pudo contactar a la API Java de vehículos.',
      // 503: servicio de vehículos temporalmente fuera de servicio.
      503: 'HTTP 503: El servicio de vehículos no está disponible.',
      // 504: timeout agotado en NGINX esperando respuesta de vehículos [Actividad 11].
      504: 'HTTP 504: Tiempo de espera agotado al consultar vehículos.',
    };

    // Construye una instancia Error usando el mensaje mapeado o uno genérico con el código HTTP exacto.
    const error = new Error(
      mensajes[respuesta.status] ?? `La API devolvió HTTP ${respuesta.status}.`,
    );
    // Adjunta el código HTTP real a la excepción para que la interfaz lo exhiba en el banner de estado [Actividad 11].
    error.status = respuesta.status; // La interfaz podrá mostrar el código.
    // Lanza el error para detener el flujo sin inventar vehículos ficticios [Actividad 11].
    throw error;
  }

  // Una API puede responder HTTP 204 No Content cuando el listado está vacío [Actividad 7].
  if (respuesta.status === 204) {
    return { vehiculos: [], codigoHttp: 204 };
  }

  // Variable donde se guardará el JSON ya deserializado.
  let datos;
  try {
    // Convierte el cuerpo JSON de la respuesta en un arreglo de objetos JavaScript.
    datos = await respuesta.json();
  } catch {
    // Informa error controlado si el servidor devolvió contenido no interpretable como JSON.
    throw new Error('La API respondió, pero el JSON no pudo interpretarse.');
  }

  // Contrato de esta práctica: GET /api/vehiculos devuelve una lista JSON (arreglo).
  // Valida estrictamente el contrato antes de entregar los datos a la vista [Actividad 7].
  if (!Array.isArray(datos)) {
    throw new Error('Se esperaba una lista JSON de vehículos (un arreglo).');
  }

  // Retorna el catálogo real recibido junto con el código de estado HTTP (ej. 200) [Actividad 7 / Actividad 10 - P27].
  return { vehiculos: datos, codigoHttp: respuesta.status };
}
