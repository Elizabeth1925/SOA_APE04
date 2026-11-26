// =================================================================================================
// Archivo: Models/CompradorDto.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Define los contratos de transferencia de datos (DTO) para la entidad Comprador,
//            tanto para la lectura de registros devueltos por GET /api/compradores como para el
//            envío de solicitudes de alta mediante POST /api/compradores hacia Python FastAPI
//            consumido a través del gateway NGINX (:8090).
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Contratos DTO y definición de entidades REST (CompradorDto y CrearCompradorRequest).
//   - [Actividad 5]: Soporte tipado a la pestaña Compradores (GET/POST /api/compradores) en Windows Forms.
//   - [Actividad 10 - P27]: Coherencia transversal de datos al asegurar que los compradores registrados
//                           desde Windows Forms cumplan el contrato esperado por FastAPI y la PWA.
// =================================================================================================

// [Actividad 1] Importa los atributos de serialización JSON de System.Text.Json para mapear propiedades C# con claves JSON.
using System.Text.Json.Serialization;

// [Actividad 1] Espacio de nombres que agrupa todos los contratos DTO del cliente Windows Forms.
namespace ConsultaVehiculosForms.Models
{
    /// <summary>
    /// [Actividad 1] [Actividad 5] Contrato DTO de lectura que representa un comprador registrado
    /// en el microservicio Python FastAPI (respuesta de GET /api/compradores y POST /api/compradores).
    /// </summary>
    public class CompradorDto
    {
        // [Actividad 1] Mapea la clave JSON "id" devuelta por FastAPI al identificador numérico del comprador.
        [JsonPropertyName("id")]
        // [Actividad 1] Identificador único autogenerado del comprador en la base de datos de ventas.
        public int Id { get; set; }

        // [Actividad 1] Mapea la clave JSON "usuario" correspondiente al usuario autenticado o asociado al registro.
        [JsonPropertyName("usuario")]
        // [Actividad 1] Nombre de usuario vinculado al comprador (admite valor nulo si el backend no lo asigna).
        public string? Usuario { get; set; }

        // [Actividad 1] Mapea la clave JSON "documento" al número de identificación del comprador.
        [JsonPropertyName("documento")]
        // [Actividad 1] Documento de identidad (cédula/RUC/pasaporte) del comprador registrado.
        public string? Documento { get; set; }

        // [Actividad 1] Mapea la clave JSON "nombre" al nombre completo del comprador.
        [JsonPropertyName("nombre")]
        // [Actividad 1] Nombre y apellido del comprador mostrado en el DataGridView administrativo.
        public string? Nombre { get; set; }

        // [Actividad 1] Mapea la clave JSON "correo" a la dirección electrónica de contacto y notificación.
        [JsonPropertyName("correo")]
        // [Actividad 1] Correo electrónico del comprador utilizado para notificaciones post-venta.
        public string? Correo { get; set; }
    }

    /// <summary>
    /// [Actividad 1] [Actividad 5] [Actividad 10 - P27] Contrato DTO de entrada para registrar un nuevo
    /// comprador mediante POST /api/compradores vía NGINX (:8090), dejándolo disponible para el flujo
    /// transversal de ventas en la PWA móvil.
    /// </summary>
    public class CrearCompradorRequest
    {
        // [Actividad 1] Serializa la propiedad Documento bajo la clave JSON exacta "documento".
        [JsonPropertyName("documento")]
        // [Actividad 1] [Actividad 5] Número de documento obligatorio del comprador; inicializado vacío para evitar nulos.
        public string Documento { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Nombre bajo la clave JSON exacta "nombre".
        [JsonPropertyName("nombre")]
        // [Actividad 1] [Actividad 5] Nombre completo obligatorio del comprador; inicializado vacío para evitar nulos.
        public string Nombre { get; set; } = string.Empty;

        // [Actividad 1] Serializa la propiedad Correo bajo la clave JSON exacta "correo".
        [JsonPropertyName("correo")]
        // [Actividad 1] [Actividad 5] Correo electrónico obligatorio del comprador; inicializado vacío para evitar nulos.
        public string Correo { get; set; } = string.Empty;
    }
}
