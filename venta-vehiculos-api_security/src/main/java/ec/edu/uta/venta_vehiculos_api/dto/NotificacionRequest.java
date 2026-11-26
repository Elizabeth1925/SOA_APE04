/**
 * Archivo: NotificacionRequest.java
 * Responsabilidad arquitectónica:
 *   Objeto de Transferencia de Datos (DTO) que modela la carga útil JSON enviada
 *   por NotificacionClient hacia el microservicio independiente de notificaciones
 *   (:8081) cuando ocurre un evento CRUD sobre el catálogo de vehículos.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Define el contrato de datos saliente asociado a las operaciones
 *     REST de vehículos.
 *   - [Actividad 2]: Soporta la integración desacoplada con el microservicio externo
 *     de notificaciones parametrizado en la configuración.
 */

// Identifica el paquete que agrupa los DTOs de entrada y salida de la API.
package ec.edu.uta.venta_vehiculos_api.dto;

// [Actividad 1] DTO serializable a JSON que encapsula la solicitud enviada a POST /api/notificaciones.
public class NotificacionRequest {

    // Identificador numérico del vehículo que originó el evento notificado.
    private Integer vehiculoId;
    // Dirección o identificador del destinatario configurado para recibir el aviso.
    private String destinatario;
    // Categoría del evento de negocio (por ejemplo: REGISTRO, ACTUALIZACION, ELIMINACION).
    private String tipo;
    // Mensaje descriptivo que detalla la acción ejecutada sobre el vehículo.
    private String mensaje;

    // Constructor vacío requerido por Jackson para serialización y deserialización JSON.
    public NotificacionRequest() {
    }

    // Constructor completo utilizado por NotificacionClient al crear el cuerpo de cada notificación.
    public NotificacionRequest(
            // Identificador del vehículo relacionado con el evento.
            Integer vehiculoId,
            // Destinatario configurado para el envío del aviso.
            String destinatario,
            // Tipo o categoría del evento ocurrido.
            String tipo,
            // Texto descriptivo del evento a notificar.
            String mensaje) {

        // Asigna el identificador del vehículo al atributo de instancia.
        this.vehiculoId = vehiculoId;
        // Asigna el destinatario al atributo de instancia.
        this.destinatario = destinatario;
        // Asigna el tipo de notificación al atributo de instancia.
        this.tipo = tipo;
        // Asigna el contenido textual del mensaje al atributo de instancia.
        this.mensaje = mensaje;
    }

    // Devuelve el identificador del vehículo asociado a la notificación.
    public Integer getVehiculoId() {
        // Retorna el valor almacenado en vehiculoId.
        return vehiculoId;
    }

    // Establece o actualiza el identificador del vehículo asociado.
    public void setVehiculoId(Integer vehiculoId) {
        // Actualiza el campo vehiculoId con el argumento recibido.
        this.vehiculoId = vehiculoId;
    }

    // Devuelve el destinatario configurado en la solicitud de notificación.
    public String getDestinatario() {
        // Retorna el valor almacenado en destinatario.
        return destinatario;
    }

    // Establece o actualiza el destinatario de la notificación.
    public void setDestinatario(String destinatario) {
        // Actualiza el campo destinatario con el argumento recibido.
        this.destinatario = destinatario;
    }

    // Devuelve el tipo o categoría del evento de notificación.
    public String getTipo() {
        // Retorna el valor almacenado en tipo.
        return tipo;
    }

    // Establece o actualiza el tipo de notificación.
    public void setTipo(String tipo) {
        // Actualiza el campo tipo con el argumento recibido.
        this.tipo = tipo;
    }

    // Devuelve el texto descriptivo del mensaje de notificación.
    public String getMensaje() {
        // Retorna el valor almacenado en mensaje.
        return mensaje;
    }

    // Establece o actualiza el texto del mensaje de notificación.
    public void setMensaje(String mensaje) {
        // Actualiza el campo mensaje con el argumento recibido.
        this.mensaje = mensaje;
    }
}
