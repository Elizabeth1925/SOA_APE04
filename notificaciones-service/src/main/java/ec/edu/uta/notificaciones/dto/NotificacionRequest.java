/**
 * ==============================================================================
 * ARCHIVO: NotificacionRequest.java
 * COMPONENTE: notificaciones-service (DTO de Entrada HTTP POST)
 * ROL: Modela la carga util JSON recibida en POST /api/notificaciones cuando un
 *      servicio autorizado envia un aviso directo sincrono.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Contrato de entrada para el registro manual
 *     o directo de notificaciones protegido por la cabecera X-Notificacion-Key.
 * ==============================================================================
 */
// Declara el paquete de objetos de transferencia de datos (DTO).
package ec.edu.uta.notificaciones.dto;

/**
 * DTO de entrada que encapsula los atributos obligatorios de una notificacion directa.
 * Todos sus campos son validados en NotificacionService.procesar().
 */
public class NotificacionRequest {

    // Identificador del vehiculo asociado al aviso (obligatorio y mayor que cero).
    private Integer vehiculoId;
    // Destinatario al que va dirigido el aviso (obligatorio y no vacio).
    private String destinatario;
    // Tipo de evento permitido: REGISTRO, ACTUALIZACION, ELIMINACION o VENTA.
    private String tipo;
    // Contenido textual de la notificacion (obligatorio y no vacio).
    private String mensaje;

    /**
     * Obtiene el identificador del vehiculo asociado.
     *
     * @return vehiculoId recibido en la solicitud.
     */
    public Integer getVehiculoId() {
        return vehiculoId;
    }

    /**
     * Establece el identificador del vehiculo asociado.
     *
     * @param vehiculoId identificador positivo del vehiculo.
     */
    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    /**
     * Obtiene el destinatario de la notificacion.
     *
     * @return nombre o direccion del destinatario.
     */
    public String getDestinatario() {
        return destinatario;
    }

    /**
     * Establece el destinatario de la notificacion.
     *
     * @param destinatario cadena identificadora del destinatario.
     */
    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    /**
     * Obtiene la categoria o tipo de notificacion solicitada.
     *
     * @return tipo de evento (REGISTRO, ACTUALIZACION, ELIMINACION o VENTA).
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Establece la categoria o tipo de notificacion solicitada.
     *
     * @param tipo categoria del aviso.
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    /**
     * Obtiene el contenido textual del mensaje.
     *
     * @return mensaje descriptivo del aviso.
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Establece el contenido textual del mensaje.
     *
     * @param mensaje texto de la notificacion.
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
