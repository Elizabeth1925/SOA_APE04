// =================================================================================================
// Archivo: Services/VehiculoApiClient.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Implementa el envoltorio genérico ApiResponse<T> y el cliente HTTP VehiculoApiClient
//            para consumir el microservicio de Vehículos (Java Spring Boot) exclusivamente a través
//            del gateway NGINX (http://127.0.0.1:8090/), adjuntando el token Bearer JWT y
//            traduciendo de forma controlada los códigos de estado HTTP (400, 401, 403, 404, 409, 5xx).
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Serialización y deserialización tipada de contratos VehiculoDto y CrearVehiculoRequest.
//   - [Actividad 5]: Consumo unificado vía NGINX (:8090), autenticación Bearer JWT, operaciones
//                    GET /api/vehiculos (con filtro opcional por marca), GET /api/vehiculos/{id},
//                    POST /api/vehiculos y manejo semántico de códigos HTTP de error.
//   - [Actividad 10 - P27]: Propagación de altas administrativas de vehículos hacia el catálogo compartido
//                           (visible en React/PWA) y reconsulta para auditar el estado VENDIDO.
//   - [Actividad 11]: Rechazo controlado de respuestas 401/403 sin desactivar la seguridad ni
//                     devolver datos ficticios ante tokens ausentes, inválidos o sin permisos.
// =================================================================================================

// [Actividad 5] Importa HttpStatusCode para evaluar y clasificar con precisión los códigos HTTP devueltos por NGINX/API.
using System.Net;
// [Actividad 5] Importa AuthenticationHeaderValue para construir la cabecera estándar Authorization: Bearer <token>.
using System.Net.Http.Headers;
// [Actividad 5] Importa Encoding.UTF8 para codificar correctamente el cuerpo JSON en peticiones POST.
using System.Text;
// [Actividad 1] [Actividad 5] Importa JsonSerializer y JsonSerializerOptions para serializar y deserializar contratos DTO.
using System.Text.Json;
// [Actividad 1] Importa los contratos DTO (VehiculoDto y CrearVehiculoRequest) definidos en la capa Models.
using ConsultaVehiculosForms.Models;

// [Actividad 5] Espacio de nombres que agrupa los clientes HTTP de consumo hacia el gateway NGINX (:8090).
namespace ConsultaVehiculosForms.Services
{
    /// <summary>
    /// [Actividad 5] [Actividad 11] Envoltorio genérico que encapsula el resultado de cada petición HTTP,
    /// separando éxito/fallo, código HTTP, carga útil tipada y mensaje descriptivo de error para la UI.
    /// </summary>
    public class ApiResponse<T>
    {
        // [Actividad 5] Indica si la respuesta HTTP estuvo en el rango exitoso 2xx (true) o falló (false).
        public bool IsSuccess { get; set; }
        // [Actividad 5] [Actividad 11] Almacena el código de estado HTTP exacto (200, 201, 400, 401, 403, 404, 409, 500, etc.).
        public HttpStatusCode StatusCode { get; set; }
        // [Actividad 1] [Actividad 11] Contiene el DTO deserializado en caso de éxito; permanece null en rechazos 401/403 (sin datos ficticios).
        public T? Data { get; set; }
        // [Actividad 5] [Actividad 11] Mensaje amigable en español con el diagnóstico del error HTTP o de conectividad.
        public string? ErrorMessage { get; set; }
    }

    /// <summary>
    /// [Actividad 5] Cliente HTTP encargado de consumir los endpoints del servicio de vehículos
    /// exclusivamente a través del gateway unificado NGINX (:8090).
    /// </summary>
    public class VehiculoApiClient
    {
        // [Actividad 5] Instancia estática reutilizable de HttpClient configurada contra el gateway unificado NGINX (:8090).
        private static readonly HttpClient _httpClient = new HttpClient
        {
            // [Actividad 5] Dirección base única del gateway NGINX en el puerto 8090 (evita llamadas directas a puertos internos).
            BaseAddress = new Uri("http://127.0.0.1:8090/"),
            // [Actividad 5] Tiempo máximo de espera de 10 segundos para prevenir bloqueos indefinidos en la interfaz gráfica.
            Timeout = TimeSpan.FromSeconds(10)
        };

        // [Actividad 1] Configuración reutilizable del serializador JSON para tolerar diferencias de mayúsculas/minúsculas en propiedades.
        private static readonly JsonSerializerOptions _jsonOptions = new JsonSerializerOptions
        {
            // [Actividad 1] Habilita el mapeo insensible a mayúsculas/minúsculas al deserializar respuestas JSON.
            PropertyNameCaseInsensitive = true
        };

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Consulta la lista de vehículos (GET /api/vehiculos vía NGINX :8090),
        /// permitiendo filtrar opcionalmente por marca y auditar el estado actual (DISPONIBLE/VENDIDO) de cada unidad.
        /// </summary>
        public async Task<ApiResponse<List<VehiculoDto>>> ListarVehiculosAsync(string token, string? marca = null)
        {
            // [Actividad 5] Define la ruta relativa base del recurso REST de vehículos en el gateway NGINX.
            var url = "api/vehiculos";
            // [Actividad 5] Verifica si el usuario administrativo especificó un filtro por marca no vacío.
            if (!string.IsNullOrWhiteSpace(marca))
            {
                // [Actividad 5] Agrega el parámetro de consulta ?marca=... codificado de forma segura para URL.
                url += $"?marca={Uri.EscapeDataString(marca.Trim())}";
            }

            // [Actividad 5] [Actividad 11] Ejecuta la solicitud GET enviando el token Bearer JWT y deserializando la lista de VehiculoDto.
            return await EnviarPeticionAsync<List<VehiculoDto>>(HttpMethod.Get, url, token);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Consulta el detalle de un vehículo específico por su ID
        /// (GET /api/vehiculos/{id} vía NGINX :8090), útil para auditar su cambio de estado post-venta a VENDIDO.
        /// </summary>
        public async Task<ApiResponse<VehiculoDto>> ObtenerVehiculoPorIdAsync(string token, int id)
        {
            // [Actividad 5] Construye la ruta relativa del recurso individual /api/vehiculos/{id}.
            var url = $"api/vehiculos/{id}";
            // [Actividad 5] [Actividad 11] Envía la petición GET autenticada y retorna el detalle del vehículo o el error HTTP mapeado.
            return await EnviarPeticionAsync<VehiculoDto>(HttpMethod.Get, url, token);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Registra un nuevo vehículo en la API Java Spring Boot
        /// (POST /api/vehiculos vía NGINX :8090), dejándolo visible de forma transversal en React y la PWA.
        /// </summary>
        public async Task<ApiResponse<VehiculoDto>> CrearVehiculoAsync(string token, CrearVehiculoRequest nuevo)
        {
            // [Actividad 1] [Actividad 5] Serializa el contrato DTO CrearVehiculoRequest a cadena JSON.
            var json = JsonSerializer.Serialize(nuevo, _jsonOptions);
            // [Actividad 5] Empaqueta el JSON en un StringContent UTF-8 con tipo MIME application/json.
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            // [Actividad 5] [Actividad 11] Ejecuta POST /api/vehiculos con Bearer JWT y retorna el vehículo creado (201) o el rechazo controlado.
            return await EnviarPeticionAsync<VehiculoDto>(HttpMethod.Post, "api/vehiculos", token, content);
        }

        /// <summary>
        /// [Actividad 5] [Actividad 11] Método genérico interno que construye el HttpRequestMessage,
        /// inyecta la cabecera Authorization: Bearer {token}, procesa la deserialización DTO y traduce
        /// de forma controlada los códigos HTTP (400, 401, 403, 404, 409, 500) y errores de red.
        /// </summary>
        private async Task<ApiResponse<T>> EnviarPeticionAsync<T>(HttpMethod method, string url, string token, HttpContent? content = null)
        {
            // [Actividad 5] Inicializa el objeto de respuesta estandarizado que recibirá la capa de interfaz gráfica.
            var response = new ApiResponse<T>();

            // [Actividad 5] Bloque protegido para capturar excepciones de timeout, red o serialización sin cerrar la aplicación.
            try
            {
                // [Actividad 5] Crea la petición HTTP desechable con el verbo y la ruta relativa sobre NGINX (:8090).
                using var request = new HttpRequestMessage(method, url);

                // [Actividad 5] Si la operación incluye cuerpo (ej. POST /api/vehiculos), lo adjunta al mensaje HTTP.
                if (content != null)
                {
                    // [Actividad 5] Asigna el payload JSON al contenido de la solicitud saliente.
                    request.Content = content;
                }

                // [Actividad 5] [Actividad 11] Si se ingresó un token JWT, lo adjunta como Bearer; si está vacío, se omite para probar el rechazo 401 del servidor.
                if (!string.IsNullOrWhiteSpace(token))
                {
                    // [Actividad 5] Elimina espacios en blanco sobrantes al inicio y final del token pegado por el usuario.
                    var cleanToken = token.Trim();
                    // [Actividad 5] Detecta si el usuario pegó el prefijo "Bearer " para evitar duplicarlo en la cabecera Authorization.
                    if (cleanToken.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
                    {
                        // [Actividad 5] Extrae únicamente el JWT limpio removiendo los 7 caracteres del prefijo "Bearer ".
                        cleanToken = cleanToken.Substring(7).Trim();
                    }
                    // [Actividad 5] Configura la cabecera estándar Authorization: Bearer <JWT> requerida por la seguridad OAuth2/JWT.
                    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cleanToken);
                }

                // [Actividad 5] Envía la petición HTTP de manera asíncrona a través de NGINX (:8090) y libera la respuesta al finalizar.
                using var httpResponse = await _httpClient.SendAsync(request);
                // [Actividad 5] [Actividad 11] Registra el código de estado HTTP exacto devuelto por el servidor.
                response.StatusCode = httpResponse.StatusCode;

                // [Actividad 5] Evalúa si el servidor respondió con un código de éxito (200 OK, 201 Created, etc.).
                if (httpResponse.IsSuccessStatusCode)
                {
                    // [Actividad 5] Lee el cuerpo de respuesta JSON de forma asíncrona.
                    var json = await httpResponse.Content.ReadAsStringAsync();
                    // [Actividad 1] Verifica que exista contenido JSON antes de intentar deserializarlo al contrato DTO.
                    if (!string.IsNullOrWhiteSpace(json))
                    {
                        // [Actividad 1] Deserializa la respuesta JSON en el tipo DTO solicitado (VehiculoDto o List<VehiculoDto>).
                        response.Data = JsonSerializer.Deserialize<T>(json, _jsonOptions);
                    }
                    // [Actividad 5] Marca la operación como exitosa para que Form1 actualice la grilla y el estado visual.
                    response.IsSuccess = true;
                }
                else
                {
                    // [Actividad 5] [Actividad 11] Marca la operación como fallida manteniendo response.Data en null (sin datos ficticios).
                    response.IsSuccess = false;
                    // [Actividad 5] Lee el detalle de error enviado en el cuerpo HTTP por el microservicio o el gateway.
                    var bodyError = await httpResponse.Content.ReadAsStringAsync();
                    // [Actividad 5] [Actividad 11] Traduce cada código de estado HTTP a un mensaje claro para el usuario administrativo.
                    response.ErrorMessage = httpResponse.StatusCode switch
                    {
                        // [Actividad 11] Rechazo controlado 401 Unauthorized por ausencia, invalidez o expiración del JWT.
                        HttpStatusCode.Unauthorized => "Error 401: No autorizado. El token no fue proporcionado, es inválido o ha expirado.",
                        // [Actividad 11] Rechazo controlado 403 Forbidden por insuficiencia de roles (se requiere OPERADOR/ADMIN).
                        HttpStatusCode.Forbidden => "Error 403: Acceso denegado. El token no tiene los permisos o roles requeridos (OPERADOR/ADMIN).",
                        // [Actividad 5] Manejo visual 404 Not Found cuando el vehículo consultado por ID no existe.
                        HttpStatusCode.NotFound => "Error 404: El recurso solicitado no existe en el servidor.",
                        // [Actividad 5] Manejo visual 400 Bad Request ante fallos de validación de entrada en la API Java.
                        HttpStatusCode.BadRequest => $"Error 400: Datos de entrada inválidos. {bodyError}",
                        // [Actividad 5] Manejo visual 409 Conflict ante duplicidad de placa/chasis o conflicto de estado.
                        HttpStatusCode.Conflict => $"Error 409: Conflicto en el registro. {bodyError}",
                        // [Actividad 5] Manejo visual 500 Internal Server Error ante fallos internos del backend Java Spring Boot.
                        HttpStatusCode.InternalServerError => "Error 500: Error interno en el servicio Java.",
                        // [Actividad 5] Captura cualquier otro código de error HTTP (incluidos otros 5xx como 502/503/504 de NGINX).
                        _ => $"Error HTTP {(int)httpResponse.StatusCode}: {httpResponse.ReasonPhrase} - {bodyError}"
                    };
                }
            }
            // [Actividad 5] Captura vencimiento del tiempo de espera (Timeout de 10 s) o cancelación de la petición.
            catch (TaskCanceledException)
            {
                // [Actividad 5] Marca la operación como no exitosa sin entregar datos simulados.
                response.IsSuccess = false;
                // [Actividad 5] Asigna código HTTP 408 RequestTimeout para indicar agotamiento del tiempo de respuesta.
                response.StatusCode = HttpStatusCode.RequestTimeout;
                // [Actividad 5] Informa al operador que verifique disponibilidad de NGINX y de la API de Vehículos.
                response.ErrorMessage = "Tiempo de espera agotado. Verifique que NGINX y la API de Vehículos estén activos.";
            }
            // [Actividad 5] Captura fallos de red o conexión rechazada al intentar alcanzar NGINX en el puerto 8090.
            catch (HttpRequestException ex)
            {
                // [Actividad 5] Marca la operación como fallida ante indisponibilidad de red.
                response.IsSuccess = false;
                // [Actividad 5] Expone el detalle de conectividad recordando que el punto de acceso es NGINX (:8090).
                response.ErrorMessage = $"Error de comunicación HTTP: {ex.Message}. Asegúrese de que NGINX esté corriendo en el puerto 8090.";
            }
            // [Actividad 5] Captura defensivamente cualquier otra excepción inesperada (ej. JSON malformado).
            catch (Exception ex)
            {
                // [Actividad 5] Marca la respuesta como fallida preservando estabilidad de la aplicación de escritorio.
                response.IsSuccess = false;
                // [Actividad 5] Almacena el mensaje de la excepción no controlada para mostrarlo en la barra de estado.
                response.ErrorMessage = $"Excepción no controlada: {ex.Message}";
            }

            // [Actividad 5] [Actividad 11] Devuelve el envoltorio ApiResponse<T> con el resultado o el error controlado a Form1.
            return response;
        }
    }
}
