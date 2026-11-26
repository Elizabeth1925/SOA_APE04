// =================================================================================================
// Archivo: Services/VentaApiClient.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Implementa el cliente HTTP VentaApiClient para consumir los endpoints del microservicio
//            Python FastAPI (Compradores y Ventas) exclusivamente a través del gateway NGINX (:8090),
//            adjuntando el token Bearer JWT y controlando todos los códigos HTTP de error.
//            RESTRICCIÓN ARQUITECTÓNICA EXPLÍCITA: Este cliente SOLO implementa consultas de ventas
//            (GET /api/ventas y GET /api/ventas/{id}) y PROHÍBE implementar o invocar POST /api/ventas,
//            operación reservada exclusivamente para la PWA móvil.
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Serialización y deserialización tipada de CompradorDto, CrearCompradorRequest y VentaDto.
//   - [Actividad 5]: Consumo unificado vía NGINX (:8090), envío de Bearer JWT, pestaña Compradores
//                    (GET/POST /api/compradores), pestaña Ventas en modo SOLO CONSULTA (GET /api/ventas
//                    y GET /api/ventas/{id}, sin POST /api/ventas) y manejo de códigos 400/401/403/404/409/5xx.
//   - [Actividad 10 - P27]: Coherencia transversal de compradores dados de alta y auditoría post-venta
//                           del estado de ventas y notificaciones generadas desde la PWA.
//   - [Actividad 11]: Rechazo controlado 401/403 sin desactivar seguridad ni retornar datos ficticios.
// =================================================================================================

// [Actividad 5] Importa HttpStatusCode para mapear semánticamente cada código HTTP retornado por NGINX/FastAPI.
using System.Net;
// [Actividad 5] Importa AuthenticationHeaderValue para construir la cabecera Authorization: Bearer <token>.
using System.Net.Http.Headers;
// [Actividad 5] Importa Encoding.UTF8 para enviar cuerpos JSON en peticiones POST /api/compradores.
using System.Text;
// [Actividad 1] [Actividad 5] Importa JsonSerializer para serializar peticiones y deserializar DTOs JSON.
using System.Text.Json;
// [Actividad 1] Importa los contratos DTO (CompradorDto, CrearCompradorRequest y VentaDto).
using ConsultaVehiculosForms.Models;

// [Actividad 5] Espacio de nombres que agrupa los servicios cliente HTTP hacia NGINX (:8090).
namespace ConsultaVehiculosForms.Services
{
    /// <summary>
    /// [Actividad 5] Cliente HTTP para consumir los endpoints del servicio Python FastAPI de compradores
    /// (GET/POST /api/compradores) y ventas en modo SOLO CONSULTA (GET /api/ventas y GET /api/ventas/{id})
    /// a través del gateway NGINX (:8090). Prohibido exponer POST /api/ventas en este cliente.
    /// </summary>
    public class VentaApiClient
    {
        // [Actividad 5] Instancia estática reutilizable de HttpClient apuntando exclusivamente al gateway NGINX (:8090).
        private static readonly HttpClient _httpClient = new HttpClient
        {
            // [Actividad 5] URL base del proxy inverso NGINX (:8090) que enruta /api/compradores y /api/ventas hacia FastAPI (:8002).
            BaseAddress = new Uri("http://127.0.0.1:8090/"),
            // [Actividad 5] Límite de espera de 10 segundos por operación para evitar congelamiento de la UI.
            Timeout = TimeSpan.FromSeconds(10)
        };

        // [Actividad 1] Opciones reutilizables de deserialización JSON tolerantes a variaciones de capitalización.
        private static readonly JsonSerializerOptions _jsonOptions = new JsonSerializerOptions
        {
            // [Actividad 1] Permite emparejar claves JSON con propiedades DTO sin sensibilidad a mayúsculas/minúsculas.
            PropertyNameCaseInsensitive = true
        };

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Registra un nuevo comprador en FastAPI (POST /api/compradores
        /// vía NGINX :8090), dejándolo disponible para operaciones de venta en el ecosistema multicanal.
        /// </summary>
        public async Task<ApiResponse<CompradorDto>> CrearCompradorAsync(string token, CrearCompradorRequest request)
        {
            // [Actividad 1] [Actividad 5] Serializa el DTO CrearCompradorRequest a formato JSON.
            var json = JsonSerializer.Serialize(request, _jsonOptions);
            // [Actividad 5] Crea el contenido HTTP UTF-8 con encabezado Content-Type: application/json.
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            // [Actividad 5] [Actividad 11] Ejecuta POST /api/compradores enviando el Bearer JWT y retorna el comprador creado o el rechazo controlado.
            return await EnviarPeticionAsync<CompradorDto>(HttpMethod.Post, "api/compradores", token, content);
        }

        /// <summary>
        /// [Actividad 5] Lista todos los compradores registrados en FastAPI (GET /api/compradores vía NGINX :8090).
        /// </summary>
        public async Task<ApiResponse<List<CompradorDto>>> ListarCompradoresAsync(string token)
        {
            // [Actividad 5] [Actividad 11] Invoca GET /api/compradores con Bearer JWT para poblar el catálogo administrativo de compradores.
            return await EnviarPeticionAsync<List<CompradorDto>>(HttpMethod.Get, "api/compradores", token);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Lista el histórico general de ventas realizadas (GET /api/ventas
        /// vía NGINX :8090) en modo SOLO CONSULTA para seguimiento y auditoría administrativa.
        /// (POST /api/ventas está explícitamente prohibido en Windows Forms; pertenece solo a la PWA).
        /// </summary>
        public async Task<ApiResponse<List<VentaDto>>> ListarVentasAsync(string token)
        {
            // [Actividad 5] [Actividad 11] Ejecuta GET /api/ventas con Bearer JWT y devuelve la lista de VentaDto o el error HTTP controlado.
            return await EnviarPeticionAsync<List<VentaDto>>(HttpMethod.Get, "api/ventas", token);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Consulta una venta individual por su ID (GET /api/ventas/{id}
        /// vía NGINX :8090) en modo SOLO CONSULTA para auditar estado, notificación y vehículo vendido.
        /// </summary>
        public async Task<ApiResponse<VentaDto>> ObtenerVentaPorIdAsync(string token, int idVenta)
        {
            // [Actividad 5] [Actividad 11] Ejecuta GET /api/ventas/{idVenta} con Bearer JWT y retorna el detalle VentaDto o el rechazo HTTP.
            return await EnviarPeticionAsync<VentaDto>(HttpMethod.Get, $"api/ventas/{idVenta}", token);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 11] Método genérico interno que arma el mensaje HTTP, adjunta la
        /// cabecera Authorization: Bearer {token}, deserializa el contrato DTO y mapea respuestas de error
        /// (400, 401, 403, 404, 409, 500 y fallos de red) sin desactivar la seguridad ni inventar datos.
        /// </summary>
        private async Task<ApiResponse<T>> EnviarPeticionAsync<T>(HttpMethod method, string url, string token, HttpContent? content = null)
        {
            // [Actividad 5] Crea el contenedor de respuesta que encapsulará estado, DTO y mensaje de error.
            var response = new ApiResponse<T>();

            // [Actividad 5] Protege el ciclo HTTP completo contra timeouts, cortes de red o excepciones inesperadas.
            try
            {
                // [Actividad 5] Construye la solicitud HTTP desechable con el método y la ruta relativa sobre NGINX (:8090).
                using var request = new HttpRequestMessage(method, url);

                // [Actividad 5] Si existe cuerpo JSON (ej. POST /api/compradores), lo asocia a la solicitud.
                if (content != null)
                {
                    // [Actividad 5] Asigna el contenido serializado al mensaje HTTP saliente.
                    request.Content = content;
                }

                // [Actividad 5] [Actividad 11] Inyecta Authorization: Bearer solo si hay token; si falta, permite validar el rechazo 401 del backend.
                if (!string.IsNullOrWhiteSpace(token))
                {
                    // [Actividad 5] Limpia espacios extremos del token ingresado en el cuadro de texto.
                    var cleanToken = token.Trim();
                    // [Actividad 5] Comprueba si el usuario incluyó manualmente el prefijo "Bearer " para normalizarlo.
                    if (cleanToken.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
                    {
                        // [Actividad 5] Remueve el prefijo "Bearer " (7 caracteres) conservando solo la cadena JWT.
                        cleanToken = cleanToken.Substring(7).Trim();
                    }
                    // [Actividad 5] Establece la cabecera HTTP Authorization con esquema Bearer y el JWT depurado.
                    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cleanToken);
                }

                // [Actividad 5] Envía la solicitud de forma asíncrona al gateway NGINX (:8090) y espera la respuesta de FastAPI.
                using var httpResponse = await _httpClient.SendAsync(request);
                // [Actividad 5] [Actividad 11] Copia el código HTTP devuelto por el servidor para trazabilidad y manejo visual.
                response.StatusCode = httpResponse.StatusCode;

                // [Actividad 5] Comprueba si el código HTTP indica operación exitosa (rango 200-299).
                if (httpResponse.IsSuccessStatusCode)
                {
                    // [Actividad 5] Lee la carga útil JSON de la respuesta de forma asíncrona.
                    var json = await httpResponse.Content.ReadAsStringAsync();
                    // [Actividad 1] Verifica que el cuerpo no esté vacío antes de deserializar el DTO.
                    if (!string.IsNullOrWhiteSpace(json))
                    {
                        // [Actividad 1] Deserializa la cadena JSON al contrato DTO correspondiente (CompradorDto o VentaDto).
                        response.Data = JsonSerializer.Deserialize<T>(json, _jsonOptions);
                    }
                    // [Actividad 5] Confirma éxito de la llamada HTTP.
                    response.IsSuccess = true;
                }
                else
                {
                    // [Actividad 5] [Actividad 11] Marca fallo y mantiene Data en null para nunca mostrar datos ficticios ante errores o rechazos.
                    response.IsSuccess = false;
                    // [Actividad 5] Extrae el cuerpo de error retornado por FastAPI o NGINX para complementar el diagnóstico.
                    var bodyError = await httpResponse.Content.ReadAsStringAsync();
                    // [Actividad 5] [Actividad 11] Asigna un mensaje claro por cada código de estado HTTP relevante.
                    response.ErrorMessage = httpResponse.StatusCode switch
                    {
                        // [Actividad 11] Rechazo controlado 401 Unauthorized cuando el token JWT falta, expiró o es inválido.
                        HttpStatusCode.Unauthorized => "Error 401: No autorizado. El token no fue proporcionado o es inválido.",
                        // [Actividad 11] Rechazo controlado 403 Forbidden cuando el rol del token no autoriza la acción solicitada.
                        HttpStatusCode.Forbidden => "Error 403: Acceso denegado. Permisos insuficientes para esta operación.",
                        // [Actividad 5] Manejo visual 404 Not Found cuando una venta o recurso no existe en FastAPI.
                        HttpStatusCode.NotFound => "Error 404: El recurso solicitado no existe en FastAPI.",
                        // [Actividad 5] Manejo visual 400 Bad Request ante datos inválidos en la solicitud.
                        HttpStatusCode.BadRequest => $"Error 400: Datos de entrada inválidos. {bodyError}",
                        // [Actividad 5] Manejo visual 409 Conflict ante registros duplicados o conflicto de negocio.
                        HttpStatusCode.Conflict => $"Error 409: Conflicto en la operación. {bodyError}",
                        // [Actividad 5] Manejo visual 500 Internal Server Error ante fallos internos del servicio de ventas.
                        HttpStatusCode.InternalServerError => "Error 500: Error interno en el servicio de ventas.",
                        // [Actividad 5] Manejo genérico para cualquier otro error HTTP (incluidos 422 o 5xx como 502/503/504).
                        _ => $"Error HTTP {(int)httpResponse.StatusCode}: {httpResponse.ReasonPhrase} - {bodyError}"
                    };
                }
            }
            // [Actividad 5] Captura vencimientos de timeout de 10 segundos al comunicarse con NGINX/FastAPI.
            catch (TaskCanceledException)
            {
                // [Actividad 5] Marca la operación como fallida.
                response.IsSuccess = false;
                // [Actividad 5] Registra estado 408 RequestTimeout para la capa de presentación.
                response.StatusCode = HttpStatusCode.RequestTimeout;
                // [Actividad 5] Informa la causa del timeout señalando NGINX y FastAPI.
                response.ErrorMessage = "Tiempo de espera agotado. Verifique que NGINX y FastAPI estén activos.";
            }
            // [Actividad 5] Captura errores de conectividad HTTP cuando NGINX (:8090) no está accesible.
            catch (HttpRequestException ex)
            {
                // [Actividad 5] Marca la operación como no exitosa.
                response.IsSuccess = false;
                // [Actividad 5] Construye el mensaje de diagnóstico indicando el puerto 8090 de NGINX.
                response.ErrorMessage = $"Error de comunicación HTTP: {ex.Message}. Asegúrese de que NGINX esté corriendo en el puerto 8090.";
            }
            // [Actividad 5] Captura cualquier excepción imprevista para evitar cierres abruptos de la ventana.
            catch (Exception ex)
            {
                // [Actividad 5] Marca la petición como fallida.
                response.IsSuccess = false;
                // [Actividad 5] Registra la descripción de la excepción en el envoltorio de respuesta.
                response.ErrorMessage = $"Excepción no controlada: {ex.Message}";
            }

            // [Actividad 5] [Actividad 11] Retorna el resultado encapsulado hacia los manejadores de eventos en Form1.
            return response;
        }
    }
}
