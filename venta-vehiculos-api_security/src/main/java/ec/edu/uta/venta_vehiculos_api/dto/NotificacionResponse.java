// Identifica el paquete de objetos usados para intercambiar datos con otros servicios.
package ec.edu.uta.venta_vehiculos_api.dto;

// Modela los campos devueltos por el servicio externo de notificaciones.
public class NotificacionResponse {

    // Indica si el servicio de notificaciones procesó correctamente la solicitud.
    private boolean success;
    // Contiene el estado textual informado por el servicio externo.
    private String estado;
    // Contiene el mensaje descriptivo devuelto por el servicio externo.
    private String mensaje;

    // Constructor vacío que permite deserializar la respuesta JSON recibida.
    public NotificacionResponse(){

    }

    // Actualiza el indicador de éxito de la respuesta.
    public void setSuccess(boolean success) {
        this.success = success;
    }

    // Actualiza el estado informado por el servicio de notificaciones.
    public void setEstado(String estado) {
        this.estado = estado;
    }

    // Actualiza el mensaje descriptivo de la respuesta.
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    // Devuelve si la notificación se procesó correctamente.
    public boolean isSuccess() {
        return success;
    }

    // Devuelve el estado informado por el servicio externo.
    public String getEstado() {
        return estado;
    }

    // Devuelve el mensaje informado por el servicio externo.
    public String getMensaje() {
        return mensaje;
    }

    
    

    
}
