/**
 * Archivo: NotificacionResponse.java
 * Responsabilidad arquitectónica:
 *   Objeto de Transferencia de Datos (DTO) que representa la estructura de respuesta
 *   JSON devuelta por el microservicio externo de notificaciones (:8081) al procesar
 *   un aviso de evento de vehículo.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Completa el contrato de integración REST de eventos de vehículos.
 *   - [Actividad 2]: Modela el intercambio interoperable entre el servicio de vehículos
 *     y el servicio auxiliar de notificaciones.
 */

// Identifica el paquete que contiene los DTOs de intercambio de datos de la API.
package ec.edu.uta.venta_vehiculos_api.dto;

// [Actividad 1] DTO deserializable desde JSON con el resultado reportado por el servicio de notificaciones.
public class NotificacionResponse {

    // Bandera booleana que indica si el servicio externo procesó exitosamente la notificación.
    private boolean success;
    // Estado textual del envío reportado por el servicio de notificaciones.
    private String estado;
    // Mensaje descriptivo adicional devuelto por el servicio de notificaciones.
    private String mensaje;

    // Constructor sin argumentos requerido por Jackson para deserializar respuestas JSON.
    public NotificacionResponse(){

    }

    // Asigna el indicador de éxito reportado por el servicio externo.
    public void setSuccess(boolean success) {
        // Guarda el valor booleano recibido en el atributo success.
        this.success = success;
    }

    // Asigna el estado textual devuelto por el servicio de notificaciones.
    public void setEstado(String estado) {
        // Guarda la cadena recibida en el atributo estado.
        this.estado = estado;
    }

    // Asigna el mensaje descriptivo devuelto por el servicio de notificaciones.
    public void setMensaje(String mensaje) {
        // Guarda la cadena recibida en el atributo mensaje.
        this.mensaje = mensaje;
    }

    // Indica si el procesamiento remoto de la notificación fue exitoso.
    public boolean isSuccess() {
        // Retorna el valor almacenado en success.
        return success;
    }

    // Devuelve el estado textual reportado por el servicio de notificaciones.
    public String getEstado() {
        // Retorna el valor almacenado en estado.
        return estado;
    }

    // Devuelve el mensaje informativo devuelto por el servicio de notificaciones.
    public String getMensaje() {
        // Retorna el valor almacenado en mensaje.
        return mensaje;
    }

}
