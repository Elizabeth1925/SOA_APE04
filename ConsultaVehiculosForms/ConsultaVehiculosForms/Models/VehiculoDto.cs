// =================================================================================================
// Archivo: Models/VehiculoDto.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Define los contratos de transferencia de datos (DTO) para consultar y dar de alta
//            vehículos del microservicio Java Spring Boot (/api/vehiculos) a través de NGINX (:8090).
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Contratos DTO y definición de entidades REST (VehiculoDto y CrearVehiculoRequest).
//   - [Actividad 5]: Soporte tipado a la pestaña Vehículos (GET/POST /api/vehiculos, filtro por marca
//                    y búsqueda por ID) del cliente administrativo Windows Forms.
//   - [Actividad 10 - P27]: Coherencia transversal de datos: permite dar de alta vehículos visibles
//                           en React/PWA y auditar posteriormente el cambio de su Estado a VENDIDO.
// =================================================================================================

// [Actividad 1] Importa los atributos de serialización JSON para alinear los nombres de campos con la API Java Spring Boot.
using System.Text.Json.Serialization;

// [Actividad 1] Espacio de nombres que agrupa los modelos DTO del cliente administrativo.
namespace ConsultaVehiculosForms.Models
{
    /// <summary>
    /// [Actividad 1] [Actividad 5] [Actividad 10 - P27] Representa la estructura de datos transferida (DTO)
    /// del catálogo de vehículos devuelta por el servicio Java Spring Boot (GET/POST /api/vehiculos),
    /// incluyendo el campo Estado para auditar la transición post-venta a VENDIDO.
    /// </summary>
    public class VehiculoDto
    {
        // [Actividad 1] Mapea la clave JSON "idVehiculo" al identificador principal del vehículo en Java Spring Boot.
        [JsonPropertyName("idVehiculo")]
        // [Actividad 1] Identificador único del vehículo en el catálogo compartido.
        public int IdVehiculo { get; set; }

        // [Actividad 1] Mapea la clave JSON "marca" utilizada también como criterio de filtrado en GET /api/vehiculos?marca=...
        [JsonPropertyName("marca")]
        // [Actividad 1] [Actividad 5] Marca comercial del vehículo (ej. Toyota, Chevrolet).
        public string? Marca { get; set; }

        // [Actividad 1] Mapea la clave JSON "modelo" al modelo específico del vehículo.
        [JsonPropertyName("modelo")]
        // [Actividad 1] Nombre del modelo del vehículo.
        public string? Modelo { get; set; }

        // [Actividad 1] Mapea la clave JSON "placa" al número de matrícula único del vehículo.
        [JsonPropertyName("placa")]
        // [Actividad 1] Placa identificatoria del vehículo (sujeta a restricción de unicidad en la API).
        public string? Placa { get; set; }

        // [Actividad 1] Mapea la clave JSON "chasis" al número VIN/chasis de 17 caracteres.
        [JsonPropertyName("chasis")]
        // [Actividad 1] Código de chasis único del vehículo.
        public string? Chasis { get; set; }

        // [Actividad 1] Mapea la clave JSON "anio" al año de fabricación del vehículo.
        [JsonPropertyName("anio")]
        // [Actividad 1] Año de fabricación (nullable para tolerar respuestas parciales del servicio).
        public int? Anio { get; set; }

        // [Actividad 1] Mapea la clave JSON "color" al color exterior del vehículo.
        [JsonPropertyName("color")]
        // [Actividad 1] Color del vehículo registrado en el catálogo.
        public string? Color { get; set; }

        // [Actividad 1] Mapea la clave JSON "precio" al precio de lista con precisión decimal monetaria.
        [JsonPropertyName("precio")]
        // [Actividad 1] Precio de venta del vehículo en formato decimal (nullable ante respuestas parciales).
        public decimal? Precio { get; set; }

        // [Actividad 1] [Actividad 10 - P27] Mapea la clave JSON "estado" que refleja disponibilidad comercial del vehículo.
        [JsonPropertyName("estado")]
        // [Actividad 1] [Actividad 10 - P27] Estado actual del vehículo (ej. DISPONIBLE o VENDIDO tras confirmación de venta desde la PWA).
        public string? Estado { get; set; }
    }

    /// <summary>
    /// [Actividad 1] [Actividad 5] [Actividad 10 - P27] Petición DTO para dar de alta un nuevo vehículo
    /// (POST /api/vehiculos vía NGINX :8090), cuyo registro queda inmediatamente visible en los clientes React y PWA.
    /// </summary>
    public class CrearVehiculoRequest
    {
        // [Actividad 1] Serializa la propiedad Marca bajo la clave JSON "marca".
        [JsonPropertyName("marca")]
        // [Actividad 1] [Actividad 5] Marca obligatoria del nuevo vehículo; inicializada vacía para prevenir valores nulos.
        public string Marca { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Modelo bajo la clave JSON "modelo".
        [JsonPropertyName("modelo")]
        // [Actividad 1] [Actividad 5] Modelo obligatorio del nuevo vehículo; inicializado vacío.
        public string Modelo { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Placa bajo la clave JSON "placa".
        [JsonPropertyName("placa")]
        // [Actividad 1] [Actividad 5] Placa obligatoria del vehículo que debe ser única en el catálogo.
        public string Placa { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Chasis bajo la clave JSON "chasis".
        [JsonPropertyName("chasis")]
        // [Actividad 1] [Actividad 5] Número de chasis obligatorio de 17 caracteres exactos según estándar de la API.
        public string Chasis { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Anio bajo la clave JSON "anio".
        [JsonPropertyName("anio")]
        // [Actividad 1] [Actividad 5] Año de fabricación numérico validado en rango permitido.
        public int Anio { get; set; }

        // [Actividad 1] Serializa la propiedad Color bajo la clave JSON "color".
        [JsonPropertyName("color")]
        // [Actividad 1] [Actividad 5] Color del vehículo a registrar; inicializado como cadena vacía.
        public string Color { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Precio bajo la clave JSON "precio".
        [JsonPropertyName("precio")]
        // [Actividad 1] [Actividad 5] Precio monetario mayor a cero del vehículo a publicar en el catálogo.
        public decimal Precio { get; set; }
    }
}
