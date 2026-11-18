using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using ConsultaVehiculosForms.Models;

namespace ConsultaVehiculosForms.Services
{
    /// <summary>
    /// Envoltorio para encapsular el resultado de las peticiones HTTP,
    /// facilitando a la interfaz de usuario identificar estados y errores.
    /// </summary>
    public class ApiResponse<T>
    {
        public bool IsSuccess { get; set; }
        public HttpStatusCode StatusCode { get; set; }
        public T? Data { get; set; }
        public string? ErrorMessage { get; set; }
    }

    /// <summary>
    /// Cliente HTTP para consumir los endpoints del servicio de vehículos (Java Spring Boot :8080).
    /// </summary>
    public class VehiculoApiClient
    {
        private static readonly HttpClient _httpClient = new HttpClient
        {
            BaseAddress = new Uri("http://127.0.0.1:8080/"),
            Timeout = TimeSpan.FromSeconds(10)
        };

        private static readonly JsonSerializerOptions _jsonOptions = new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        };

        /// <summary>
        /// Consulta la lista de vehículos (GET /api/vehiculos), permitiendo opcionalmente filtrar por marca.
        /// </summary>
        public async Task<ApiResponse<List<VehiculoDto>>> ListarVehiculosAsync(string token, string? marca = null)
        {
            var url = "api/vehiculos";
            if (!string.IsNullOrWhiteSpace(marca))
            {
                url += $"?marca={Uri.EscapeDataString(marca.Trim())}";
            }

            return await EnviarPeticionAsync<List<VehiculoDto>>(HttpMethod.Get, url, token);
        }

        /// <summary>
        /// Consulta el detalle de un vehículo específico por su identificador (GET /api/vehiculos/{id}).
        /// </summary>
        public async Task<ApiResponse<VehiculoDto>> ObtenerVehiculoPorIdAsync(string token, int id)
        {
            var url = $"api/vehiculos/{id}";
            return await EnviarPeticionAsync<VehiculoDto>(HttpMethod.Get, url, token);
        }

        /// <summary>
        /// Método genérico que construye el HttpRequestMessage, inyecta la cabecera Authorization: Bearer {token}
        /// y captura respuestas y posibles errores de red o servidor.
        /// </summary>
        private async Task<ApiResponse<T>> EnviarPeticionAsync<T>(HttpMethod method, string url, string token)
        {
            var response = new ApiResponse<T>();

            try
            {
                using var request = new HttpRequestMessage(method, url);

                // Si se proporcionó un token, se limpia el prefijo "Bearer " si el usuario lo incluyó
                if (!string.IsNullOrWhiteSpace(token))
                {
                    var cleanToken = token.Trim();
                    if (cleanToken.StartsWith("Bearer ", StringComparison.OrdinalIgnoreCase))
                    {
                        cleanToken = cleanToken.Substring(7).Trim();
                    }
                    request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", cleanToken);
                }

                using var httpResponse = await _httpClient.SendAsync(request);
                response.StatusCode = httpResponse.StatusCode;

                if (httpResponse.IsSuccessStatusCode)
                {
                    var json = await httpResponse.Content.ReadAsStringAsync();
                    response.Data = JsonSerializer.Deserialize<T>(json, _jsonOptions);
                    response.IsSuccess = true;
                }
                else
                {
                    response.IsSuccess = false;
                    response.ErrorMessage = httpResponse.StatusCode switch
                    {
                        HttpStatusCode.Unauthorized => "Error 401: No autorizado. El token no fue proporcionado, es inválido o ha expirado.",
                        HttpStatusCode.Forbidden => "Error 403: Acceso denegado. El token no tiene los permisos o roles requeridos.",
                        HttpStatusCode.NotFound => "Error 404: El recurso solicitado no existe en el servidor.",
                        HttpStatusCode.InternalServerError => "Error 500: Error interno en el servicio Java.",
                        _ => $"Error HTTP {(int)httpResponse.StatusCode}: {httpResponse.ReasonPhrase}"
                    };
                }
            }
            catch (HttpRequestException ex)
            {
                response.IsSuccess = false;
                response.StatusCode = HttpStatusCode.ServiceUnavailable;
                response.ErrorMessage = $"Error de conexión: No se pudo conectar con el servicio Java (:8080). ¿El servicio está encendido? ({ex.Message})";
            }
            catch (TaskCanceledException)
            {
                response.IsSuccess = false;
                response.StatusCode = HttpStatusCode.RequestTimeout;
                response.ErrorMessage = "Tiempo de espera agotado (Timeout) al conectar con el servicio Java.";
            }
            catch (Exception ex)
            {
                response.IsSuccess = false;
                response.ErrorMessage = $"Error inesperado: {ex.Message}";
            }

            return response;
        }
    }
}
