// =================================================================================================
// Archivo: Models/VentaDto.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Define el contrato DTO de solo lectura VentaDto que representa una operación de venta
//            consultada desde el microservicio Python FastAPI (GET /api/ventas y GET /api/ventas/{id})
//            a través del gateway NGINX (:8090).
//            Restricción de arquitectura: No define ningún DTO de creación porque Windows Forms
//            prohíbe explícitamente ejecutar POST /api/ventas (exclusivo de la PWA móvil).
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Contratos DTO y definición de entidades REST (VentaDto).
//   - [Actividad 5]: Soporte a la pestaña Ventas en modo SOLO CONSULTA (GET /api/ventas y
//                    GET /api/ventas/{id}), prohibiendo explícitamente POST /api/ventas.
//   - [Actividad 10 - P27]: Auditoría transversal post-venta (correlación de vehiculoId, compradorId,
//                           estado de venta y estado de notificación).
// =================================================================================================

// [Actividad 1] Importa los atributos de System.Text.Json para mapear exactamente las claves JSON de FastAPI.
using System.Text.Json.Serialization;

// [Actividad 1] Espacio de nombres que contiene los contratos DTO del cliente Windows Forms.
namespace ConsultaVehiculosForms.Models
{
    /// <summary>
    /// [Actividad 1] [Actividad 5] [Actividad 10 - P27] Representa los datos de solo lectura devueltos
    /// por los endpoints GET /api/ventas y GET /api/ventas/{id} del servicio Python FastAPI (:8002)
    /// accedido mediante NGINX (:8090). No incluye contrato de alta porque POST /api/ventas está prohibido aquí.
    /// </summary>
    public class VentaDto
    {
        // [Actividad 1] Mapea la clave JSON "id" al identificador numérico de la venta.
        [JsonPropertyName("id")]
        // [Actividad 1] [Actividad 5] Identificador único de la venta usado también en consultas GET /api/ventas/{id}.
        public int Id { get; set; }

        // [Actividad 1] Mapea la clave JSON "operacionId" al UUID de trazabilidad e idempotencia de la operación.
        [JsonPropertyName("operacionId")]
        // [Actividad 1] [Actividad 10 - P27] Identificador único de operación (UUID) para seguimiento distribuido entre servicios.
        public string? OperacionId { get; set; }

        // [Actividad 1] Mapea la clave JSON "vehiculoId" al identificador del vehículo vendido en Java Spring Boot.
        [JsonPropertyName("vehiculoId")]
        // [Actividad 1] [Actividad 10 - P27] Clave de referencia al vehículo cuyo estado debe auditarse como VENDIDO.
        public int VehiculoId { get; set; }

        // [Actividad 1] Mapea la clave JSON "compradorId" al identificador del comprador registrado en FastAPI.
        [JsonPropertyName("compradorId")]
        // [Actividad 1] [Actividad 10 - P27] Clave de referencia al comprador que participó en la transacción.
        public int CompradorId { get; set; }

        // [Actividad 1] Mapea la clave JSON "precioAcordado" al monto final de la venta recibido como cadena formateada.
        [JsonPropertyName("precioAcordado")]
        // [Actividad 1] Precio acordado de la transacción de venta.
        public string? PrecioAcordado { get; set; }

        // [Actividad 1] Mapea la clave JSON "moneda" al código de divisa de la venta (ej. USD).
        [JsonPropertyName("moneda")]
        // [Actividad 1] Moneda en la que se registró el precio acordado.
        public string? Moneda { get; set; }

        // [Actividad 1] [Actividad 10 - P27] Mapea la clave JSON "estado" al resultado de la orquestación de la venta.
        [JsonPropertyName("estado")]
        // [Actividad 1] [Actividad 10 - P27] Estado de la venta (ej. COMPLETADA, EN_REVISION, FALLIDA) auditado en el panel de detalle.
        public string? Estado { get; set; }

        // [Actividad 1] [Actividad 10 - P27] Mapea la clave JSON "notificacion" al estado del envío de notificación al comprador.
        [JsonPropertyName("notificacion")]
        // [Actividad 1] [Actividad 10 - P27] Estado de la notificación asociada a la venta (ej. ENVIADA, PENDIENTE).
        public string? Notificacion { get; set; }

        // [Actividad 1] Mapea la clave JSON "fechaUTC" a la marca temporal UTC en que se procesó la venta.
        [JsonPropertyName("fechaUTC")]
        // [Actividad 1] Fecha y hora en formato UTC del registro de la venta para trazabilidad administrativa.
        public string? FechaUTC { get; set; }
    }
}
