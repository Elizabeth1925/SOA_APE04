using System.Net;
using System.Net.Http.Headers;
using System.Text.Json;
using ConsultaVehiculosForms.Models;

namespace ConsultaVehiculosForms.Services
{
    /// <summary>
    /// Cliente HTTP para consumir los endpoints del servicio de ventas (Python FastAPI :8002).
    /// </summary>
    public class VentaApiClient
    {
        private static readonly HttpClient _httpClient = new HttpClient
        {
            BaseAddress = new Uri("http://127.0.0.1:8002/"),
            Timeout = TimeSpan.FromSeconds(10)
        };

        private static readonly JsonSerializerOptions _jsonOptions = new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        };

        /// <summary>
        /// Consulta una venta por su ID (GET /api/ventas/{id}) para obtener su estado y notificación.
        /// </summary>
        public async Task<ApiResponse<VentaDto>> ObtenerVentaPorIdAsync(string token, int idVenta)
        {
            var response = new ApiResponse<VentaDto>();

            try
            {
                using var request = new HttpRequestMessage(HttpMethod.Get, $"api/ventas/{idVenta}");

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
                    response.Data = JsonSerializer.Deserialize<VentaDto>(json, _jsonOptions);
                    response.IsSuccess = true;
                }
                else
                {
                    response.IsSuccess = false;
                    response.ErrorMessage = httpResponse.StatusCode switch
                    {
                        HttpStatusCode.Unauthorized => "Error 401: No autorizado. El token no fue enviado o no es válido.",
                        HttpStatusCode.Forbidden => "Error 403: Acceso denegado. No eres propietario de esta venta ni tienes rol ADMIN.",
                        HttpStatusCode.NotFound => $"Error 404: No existe ninguna venta registrada con el ID {idVenta}.",
                        _ => $"Error HTTP {(int)httpResponse.StatusCode}: {httpResponse.ReasonPhrase}"
                    };
                }
            }
            catch (HttpRequestException ex)
            {
                response.IsSuccess = false;
                response.StatusCode = HttpStatusCode.ServiceUnavailable;
                response.ErrorMessage = $"Error de conexión con Python (:8002). ¿El servicio FastAPI está corriendo? ({ex.Message})";
            }
            catch (TaskCanceledException)
            {
                response.IsSuccess = false;
                response.StatusCode = HttpStatusCode.RequestTimeout;
                response.ErrorMessage = "Tiempo de espera agotado al consultar el servicio de ventas.";
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
