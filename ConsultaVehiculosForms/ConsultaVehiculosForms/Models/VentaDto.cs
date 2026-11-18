using System.Text.Json.Serialization;

namespace ConsultaVehiculosForms.Models
{
    /// <summary>
    /// Representa los datos devueltos por el endpoint GET /api/ventas/{id}
    /// del servicio Python FastAPI (:8002).
    /// </summary>
    public class VentaDto
    {
        [JsonPropertyName("id")]
        public int Id { get; set; }

        [JsonPropertyName("operacionId")]
        public string? OperacionId { get; set; }

        [JsonPropertyName("vehiculoId")]
        public int VehiculoId { get; set; }

        [JsonPropertyName("compradorId")]
        public int CompradorId { get; set; }

        [JsonPropertyName("precioAcordado")]
        public string? PrecioAcordado { get; set; }

        [JsonPropertyName("moneda")]
        public string? Moneda { get; set; }

        [JsonPropertyName("estado")]
        public string? Estado { get; set; }

        [JsonPropertyName("notificacion")]
        public string? Notificacion { get; set; }

        [JsonPropertyName("fechaUTC")]
        public string? FechaUTC { get; set; }
    }
}
