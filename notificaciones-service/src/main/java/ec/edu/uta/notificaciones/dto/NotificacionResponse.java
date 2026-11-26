/**
 * ==============================================================================
 * ARCHIVO: NotificacionResponse.java
 * COMPONENTE: notificaciones-service (DTO de Respuesta HTTP)
 * ROL: Estandariza las respuestas JSON tanto de exito (201 Created en POST)
 *      como de error de validacion (400 Bad Request en ApiExceptionHandler).
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Contrato uniforme de respuesta para la API
 *     REST de notificaciones.
 * ==============================================================================
 */
// Declara el paquete de objetos de transferencia de datos (DTO).
package ec.edu.uta.notificaciones.dto;

/**
 * DTO de respuesta devuelto por los endpoints de escritura y por el manejador
 * global de errores de validacion.
 */
public class NotificacionResponse {

    // Indica si la solicitud fue procesada satisfactoriamente (true) o rechazada (false).
    private boolean success;
    // Estado resultante de la operacion (por ejemplo "ENVIADA" o null en caso de error).
    private String estado;
    // Mensaje descriptivo del resultado o detalle del error de validacion.
    private String mensaje;

    /**
     * Constructor por defecto requerido por Jackson.
     */
    public NotificacionResponse() {
    }

    /**
     * Constructor parametrizado para construir una respuesta completa.
     *
     * @param success bandera de exito de la operacion.
     * @param estado  estado asignado a la notificacion o null en error.
     * @param mensaje descripcion legible del resultado.
     */
    public NotificacionResponse(boolean success, String estado, String mensaje) {
        // Asigna el indicador booleano de exito.
        this.success = success;
        // Asigna el estado textual de la notificacion.
        this.estado = estado;
        // Asigna el mensaje informativo de respuesta.
        this.mensaje = mensaje;
    }

    /**
     * Indica si la operacion concluyo con exito.
     *
     * @return true si fue exitosa; false en caso contrario.
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * Define el indicador booleano de exito.
     *
     * @param success true para exito, false para fallo.
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Obtiene el estado textual de la notificacion.
     *
     * @return estado o null si ocurrio un error de validacion.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado textual de la notificacion.
     *
     * @param estado etiqueta de estado.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene el mensaje descriptivo de la respuesta.
     *
     * @return texto de confirmacion o mensaje de error.
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Establece el mensaje descriptivo de la respuesta.
     *
     * @param mensaje texto de resultado.
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
