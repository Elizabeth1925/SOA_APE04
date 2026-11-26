/*
 * ============================================================================
 * Archivo: src/api/ventasApi.js
 * Proyecto: Portal Web React de Consulta y Auditoría (react-fetch-vehiculos) - SOA APE 04
 * Función:
 *   Cliente HTTP exclusivo de solo lectura para consultar el historial real de
 *   ventas (GET /api/ventas) vía proxy de Vite -> Gateway NGINX (:8090) ->
 *   FastAPI (:8002). Por política arquitectónica de la APE 04, este módulo
 *   prohíbe explícitamente implementar POST /api/ventas ni operaciones de
 *   mutación (exclusivas de la PWA móvil).
 * Actividades APE 04 cubiertas:
 *   - [Actividad 7]: Consumo exclusivo de GET /api/ventas para KPIs reales y
 *     auditoría individual; prohibición explícita de POST /api/ventas.
 *   - [Actividad 10 - P27]: Recuperación fresca (cache: 'no-store') de ventas
 *     reales generadas desde la PWA para verificar coherencia transversal.
 *   - [Actividad 11]: Manejo controlado de HTTP 401 (sin token o vencido),
 *     403 y fallos de gateway 502/503/504 sin fabricar ventas ficticias.
 * ============================================================================
 */
/**
 * Cliente HTTP para la consulta de ventas en React (APE 04).
 * Responsabilidad exclusiva: consulta de ventas y obtención de métricas.
 * Este cliente web NO contiene operaciones de creación/mutación de ventas (prohibido POST /api/ventas) [Actividad 7].
 */
// Limpia el token JWT eliminando espacios y cualquier prefijo "Bearer " repetido.
function normalizarToken(token = '') {
  // Normaliza el valor a texto plano sin espacios ni prefijo Bearer.
  return String(token).trim().replace(/^Bearer\s+/i, '').trim();
}

// Consulta el registro real de ventas mediante GET /api/ventas en modo estrictamente de auditoría [Actividad 7 / Actividad 10 - P27 / Actividad 11].
export async function listarVentas(token) {
  // Inicializa las cabeceras HTTP indicando que el cliente espera una respuesta JSON.
  const headers = new Headers({ Accept: 'application/json' });
  // Normaliza el token proporcionado por el usuario.
  const jwt = normalizarToken(token);
  // Adjunta la cabecera Authorization solo si existe JWT; omitirlo permite probar HTTP 401 real [Actividad 11].
  if (jwt) {
    headers.set('Authorization', `Bearer ${jwt}`);
  }

  // Variable para recibir la respuesta HTTP de fetch.
  let respuesta;
  try {
    // Invoca GET /api/ventas (nunca POST) a través del proxy Vite -> NGINX (:8090) -> FastAPI [Actividad 7].
    respuesta = await fetch('/api/ventas', {
      // Operación exclusiva de lectura/auditoría [Actividad 7].
      method: 'GET',
      // Cabeceras Accept y Authorization (cuando hay token).
      headers,
      // Impide usar caché del navegador para ver inmediatamente nuevas ventas emitidas en la PWA [Actividad 10 - P27].
      cache: 'no-store',
    });
  } catch {
    // Reporta de forma controlada cualquier fallo de transporte o caída del Gateway NGINX [Actividad 11].
    throw new Error('No se pudo conectar con el servicio de ventas a través de NGINX.');
  }

  // Verifica si el código HTTP indica error (fuera del rango 200-299) y detiene sin inventar datos [Actividad 11].
  if (!respuesta.ok) {
    // Tabla de mensajes claros asociados a cada código HTTP de seguridad o disponibilidad [Actividad 11].
    const mensajes = {
      // 401: token JWT faltante, inválido o vencido [Actividad 11].
      401: 'HTTP 401: Falta el JWT o el token ha expirado.',
      // 403: el usuario autenticado no tiene permisos de auditoría de ventas [Actividad 11].
      403: 'HTTP 403: El usuario no cuenta con permisos para auditar ventas.',
      // 404: ruta /api/ventas no hallada en el enrutador.
      404: 'HTTP 404: No se encontró el recurso /api/ventas.',
      // 500: error interno de ejecución en el microservicio de ventas FastAPI.
      500: 'HTTP 500: Error interno en el servicio de ventas (FastAPI).',
      // 502: NGINX no pudo conectar con FastAPI en el puerto 8002 [Actividad 11].
      502: 'HTTP 502: NGINX no pudo contactar a FastAPI (puerto 8002).',
      // 503: servicio de ventas temporalmente no disponible.
      503: 'HTTP 503: El servicio de ventas no está disponible.',
      // 504: tiempo de espera excedido en NGINX esperando a FastAPI [Actividad 11].
      504: 'HTTP 504: Tiempo de espera agotado al consultar ventas.',
    };
    // Crea el objeto Error con el mensaje correspondiente al estado HTTP real.
    const error = new Error(
      mensajes[respuesta.status] ?? `HTTP ${respuesta.status}: Error al consultar el registro de ventas.`,
    );
    // Preserva el código HTTP numérico en la excepción para desplegarlo en la UI [Actividad 11].
    error.status = respuesta.status;
    // Propaga el error al componente App para que muestre la alerta controlada.
    throw error;
  }

  // Maneja limpiamente una respuesta HTTP 204 (sin ventas registradas) devolviendo arreglo vacío [Actividad 7].
  if (respuesta.status === 204) {
    return { ventas: [], codigoHttp: 204 };
  }

  // Variable que contendrá la lista JSON parseada de ventas.
  let datos;
  try {
    // Decodifica la respuesta JSON del microservicio de ventas.
    datos = await respuesta.json();
  } catch {
    // Lanza error controlado si el cuerpo recibido no es JSON válido.
    throw new Error('La respuesta de ventas no contiene JSON válido.');
  }

  // Valida que el contrato de GET /api/ventas entregue efectivamente un arreglo JSON [Actividad 7].
  if (!Array.isArray(datos)) {
    throw new Error('Se esperaba un arreglo JSON con el listado de ventas.');
  }

  // Devuelve las transacciones reales obtenidas y el código HTTP real (ej. 200) [Actividad 7 / Actividad 10 - P27].
  return { ventas: datos, codigoHttp: respuesta.status };
}
