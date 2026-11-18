using System.Text.Json.Serialization;

namespace ConsultaVehiculosForms.Models
{
    /// <summary>
    /// Representa la estructura de datos transferida (DTO) del catálogo de vehículos
    /// devuelta por el servicio Java Spring Boot (/api/vehiculos).
    /// </summary>
    public class VehiculoDto
    {
        [JsonPropertyName("idVehiculo")]
        public int IdVehiculo { get; set; }

        [JsonPropertyName("marca")]
        public string? Marca { get; set; }

        [JsonPropertyName("modelo")]
        public string? Modelo { get; set; }

        [JsonPropertyName("placa")]
        public string? Placa { get; set; }

        [JsonPropertyName("chasis")]
        public string? Chasis { get; set; }

        [JsonPropertyName("anio")]
        public int? Anio { get; set; }

        [JsonPropertyName("color")]
        public string? Color { get; set; }

        [JsonPropertyName("precio")]
        public decimal? Precio { get; set; }

        [JsonPropertyName("estado")]
        public string? Estado { get; set; }
    }
}
