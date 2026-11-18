// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Representa la información que la API envía al servicio de notificaciones.
public class NotificacionRequest {

    // Identifica el vehículo relacionado con la notificación.
    private Integer vehiculoId;
    // Indica a quién debe enviarse la notificación.
    private String destinatario;
    // Describe la categoría o evento que origina la notificación.
    private String tipo;
    // Contiene el texto que se comunicará al destinatario.
    private String mensaje;

    // Constructor vacío requerido para convertir el DTO a/desde JSON.
    public NotificacionRequest() {
    }

    // Construye una solicitud de notificación con todos sus datos.
    public NotificacionRequest(
            // Identificador del vehículo relacionado.
            Integer vehiculoId,
            // Dirección o identificador del destinatario.
            String destinatario,
            // Tipo de notificación que se enviará.
            String tipo,
            // Contenido del mensaje que recibirá el destinatario.
            String mensaje) {

        // Guarda el identificador del vehículo en el objeto.
        this.vehiculoId = vehiculoId;
        // Guarda el destinatario en el objeto.
        this.destinatario = destinatario;
        // Guarda el tipo de notificación en el objeto.
        this.tipo = tipo;
        // Guarda el mensaje en el objeto.
        this.mensaje = mensaje;
    }

    // Devuelve el identificador del vehículo asociado.
    public Integer getVehiculoId() {
        return vehiculoId;
    }

    // Cambia el identificador del vehículo asociado.
    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    // Devuelve el destinatario de la notificación.
    public String getDestinatario() {
        return destinatario;
    }

    // Cambia el destinatario de la notificación.
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    // Devuelve el tipo de notificación.
    public String getTipo() {
        return tipo;
    }

    // Cambia el tipo de notificación.
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    // Devuelve el texto del mensaje.
    public String getMensaje() {
        return mensaje;
    }

    // Cambia el texto del mensaje.
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}