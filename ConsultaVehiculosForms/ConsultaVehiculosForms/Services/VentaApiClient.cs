using System.Net;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using ConsultaVehiculosForms.Models;

namespace ConsultaVehiculosForms.Services
{
    /// <summary>
    /// Cliente HTTP para consumir los endpoints del servicio de ventas y compradores a través de NGINX (:8090).
    /// </summary>
    public class VentaApiClient
    {
        private static readonly HttpClient _httpClient = new HttpClient
        {
            BaseAddress = new Uri("http://127.0.0.1:8090/"),
            Timeout = TimeSpan.FromSeconds(10)
        };

        private static readonly JsonSerializerOptions _jsonOptions = new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        };

        /// <summary>
        /// Registra un nuevo comprador en FastAPI (POST /api/compradores).
        /// </summary>
        public async Task<ApiResponse<CompradorDto>> CrearCompradorAsync(string token, CrearCompradorRequest request)
        {
            var json = JsonSerializer.Serialize(request, _jsonOptions);
            var content = new StringContent(json, Encoding.UTF8, "application/json");
            return await EnviarPeticionAsync<CompradorDto>(HttpMethod.Post, "api/compradores", token, content);
        }

        /// <summary>
        /// Lista todos los compradores registrados (GET /api/compradores).
        /// </summary>
        public async Task<ApiResponse<List<CompradorDto>>> ListarCompradoresAsync(string token)
        {
            return await EnviarPeticionAsync<List<CompradorDto>>(HttpMethod.Get, "api/compradores", token);
        }

        /// <summary>
        /// Lista el histórico general de ventas realizadas (GET /api/ventas).
        /// </summary>
        public async Task<ApiResponse<List<VentaDto>>> ListarVentasAsync(string token)
        {
            return await EnviarPeticionAsync<List<VentaDto>>(HttpMethod.Get, "api/ventas", token);
        }

        /// <summary>
        /// Consulta una venta individual por su ID (GET /api/ventas/{id}).
        /// </summary>
        public async Task<ApiResponse<VentaDto>> ObtenerVentaPorIdAsync(string token, int idVenta)
        {
            return await EnviarPeticionAsync<VentaDto>(HttpMethod.Get, $"api/ventas/{idVenta}", token);
        }

        private async Task<ApiResponse<T>> EnviarPeticionAsync<T>(HttpMethod method, string url, string token, HttpContent? content = null)
        {
            var response = new ApiResponse<T>();

            try
            {
                using var request = new HttpRequestMessage(method, url);

                if (content != null)
                {
                    request.Content = content;
                }

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
                    if (!string.IsNullOrWhiteSpace(json))
                    {
                        response.Data = JsonSerializer.Deserialize<T>(json, _jsonOptions);
                    }
                    response.IsSuccess = true;
                }
                else
                {
                    response.IsSuccess = false;
                    var bodyError = await httpResponse.Content.ReadAsStringAsync();
                    response.ErrorMessage = httpResponse.StatusCode switch
                    {
                        HttpStatusCode.Unauthorized => "Error 401: No autorizado. El token no fue proporcionado o es inválido.",
                        HttpStatusCode.Forbidden => "Error 403: Acceso denegado. Permisos insuficientes para esta operación.",
                        HttpStatusCode.NotFound => "Error 404: El recurso solicitado no existe en FastAPI.",
                        HttpStatusCode.BadRequest => $"Error 400: Datos de entrada inválidos. {bodyError}",
                        HttpStatusCode.Conflict => $"Error 409: Conflicto en la operación. {bodyError}",
                        HttpStatusCode.InternalServerError => "Error 500: Error interno en el servicio de ventas.",
                        _ => $"Error HTTP {(int)httpResponse.StatusCode}: {httpResponse.ReasonPhrase} - {bodyError}"
                    };
                }
            }
            catch (TaskCanceledException)
            {
                response.IsSuccess = false;
                response.StatusCode = HttpStatusCode.RequestTimeout;
                response.ErrorMessage = "Tiempo de espera agotado. Verifique que NGINX y FastAPI estén activos.";
            }
            catch (HttpRequestException ex)
            {
                response.IsSuccess = false;
                response.ErrorMessage = $"Error de comunicación HTTP: {ex.Message}. Asegúrese de que NGINX esté corriendo en el puerto 8090.";
            }
            catch (Exception ex)
            {
                response.IsSuccess = false;
                response.ErrorMessage = $"Excepción no controlada: {ex.Message}";
            }

            return response;
        }
    }
}
