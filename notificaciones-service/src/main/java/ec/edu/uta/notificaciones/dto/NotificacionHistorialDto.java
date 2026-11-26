/**
 * ==============================================================================
 * ARCHIVO: NotificacionHistorialDto.java
 * COMPONENTE: notificaciones-service (DTO de Salida / Historial)
 * ROL: Modela cada entrada del historial de notificaciones expuesta en formato
 *      JSON por GET /api/notificaciones para su consulta en JavaFX.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Estructura serializable almacenada en el
 *     historial concurrente (CopyOnWriteArrayList) tras consumir eventos AMQP
 *     o recibir avisos HTTP POST.
 *   - [Actividad 10 - P27]: Transporta los campos de trazabilidad de punta a
 *     punta (eventoId, ventaId, vehiculoId, compradorId, operacionId) hacia el
 *     panel de notificaciones de la aplicacion JavaFX.
 * ==============================================================================
 */
// Declara el paquete de objetos de transferencia de datos (DTO) del servicio.
package ec.edu.uta.notificaciones.dto;

// Importacion conservada del contrato original de la clase (sin alterar codigo fuente ejecutable).
import java.time.LocalDateTime;

/**
 * DTO inmutable a nivel de contrato de salida que representa una notificacion
 * registrada en el historial en memoria del microservicio.
 */
public class NotificacionHistorialDto {
    // Identificador secuencial interno generado por AtomicLong dentro de notificaciones-service.
    private Long id;
    // Identificador unico del evento (clave de deduplicacion AMQP en Actividad 11 - P22 o UUID en POST).
    private String eventoId;
    // Categoria del evento de negocio: VENTA, REGISTRO, ACTUALIZACION o ELIMINACION.
    private String tipo;
    // [Actividad 10 - P27] Identificador de la venta confirmada en ventas-api (nulo en notificaciones directas).
    private Integer ventaId;
    // Identificador del vehiculo asociado al evento de venta o notificacion directa.
    private Integer vehiculoId;
    // Identificador del comprador asignado a la venta (nulo en notificaciones directas).
    private Integer compradorId;
    // [Actividad 10 - P27] Identificador de correlacion distribuida (operacionId) que vincula reserva, venta y aviso.
    private String operacionId;
    // Texto descriptivo legible mostrado al usuario final en la tabla del cliente JavaFX.
    private String mensaje;
    // Estado final del aviso registrado (por ejemplo: PROCESADA para AMQP o ENVIADA para POST).
    private String estado;
    // Marca temporal formateada (yyyy-MM-dd HH:mm:ss) del momento de registro en el servicio.
    private String fechaRegistro;

    /**
     * Constructor sin argumentos requerido por Jackson para serializacion y deserializacion JSON.
     */
    public NotificacionHistorialDto() {
    }

    /**
     * Constructor completo para instanciar una entrada del historial con todos sus campos de trazabilidad.
     *
     * @param id            identificador incremental en memoria.
     * @param eventoId      identificador unico del evento procesado.
     * @param tipo          tipo de notificacion.
     * @param ventaId       identificador de la venta asociada.
     * @param vehiculoId    identificador del vehiculo involucrado.
     * @param compradorId   identificador del comprador asociado.
     * @param operacionId   identificador de operacion distribuida.
     * @param mensaje       texto descriptivo de la notificacion.
     * @param estado        estado de procesamiento (PROCESADA o ENVIADA).
     * @param fechaRegistro fecha y hora de registro formateada.
     */
    public NotificacionHistorialDto(Long id, String eventoId, String tipo, Integer ventaId, Integer vehiculoId, Integer compradorId, String operacionId, String mensaje, String estado, String fechaRegistro) {
        // Asigna el identificador secuencial interno.
        this.id = id;
        // Asigna el identificador unico del evento para trazabilidad y deduplicacion.
        this.eventoId = eventoId;
        // Asigna el tipo de notificacion.
        this.tipo = tipo;
        // Asigna el identificador de la venta (Actividad 10 - P27).
        this.ventaId = ventaId;
        // Asigna el identificador del vehiculo afectado.
        this.vehiculoId = vehiculoId;
        // Asigna el identificador del comprador.
        this.compradorId = compradorId;
        // Asigna el identificador de operacion distribuida (Actividad 10 - P27).
        this.operacionId = operacionId;
        // Asigna el mensaje descriptivo.
        this.mensaje = mensaje;
        // Asigna el estado del registro.
        this.estado = estado;
        // Asigna la fecha y hora de registro formateada.
        this.fechaRegistro = fechaRegistro;
    }

    /**
     * Obtiene el identificador secuencial interno de la notificacion.
     *
     * @return identificador incremental en memoria.
     */
    public Long getId() {
        return id;
    }

    /**
     * Establece el identificador secuencial interno de la notificacion.
     *
     * @param id identificador incremental en memoria.
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Obtiene el identificador unico del evento (clave de idempotencia AMQP).
     *
     * @return cadena eventoId.
     */
    public String getEventoId() {
        return eventoId;
    }

    /**
     * Establece el identificador unico del evento.
     *
     * @param eventoId cadena unica del evento.
     */
    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    /**
     * Obtiene el tipo de evento de la notificacion.
     *
     * @return tipo de notificacion (por ejemplo VENTA).
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de evento de la notificacion.
     *
     * @param tipo categoria del aviso.
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    /**
     * Obtiene el identificador de la venta confirmada (trazabilidad Actividad 10 - P27).
     *
     * @return ventaId o null si fue un aviso manual sin venta asociada.
     */
    public Integer getVentaId() {
        return ventaId;
    }

    /**
     * Establece el identificador de la venta confirmada.
     *
     * @param ventaId identificador numerico de la venta.
     */
    public void setVentaId(Integer ventaId) {
        this.ventaId = ventaId;
    }

    /**
     * Obtiene el identificador del vehiculo vinculado al aviso.
     *
     * @return vehiculoId.
     */
    public Integer getVehiculoId() {
        return vehiculoId;
    }

    /**
     * Establece el identificador del vehiculo vinculado al aviso.
     *
     * @param vehiculoId identificador numerico del vehiculo.
     */
    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    /**
     * Obtiene el identificador del comprador vinculado a la venta.
     *
     * @return compradorId o null en notificaciones manuales.
     */
    public Integer getCompradorId() {
        return compradorId;
    }

    /**
     * Establece el identificador del comprador vinculado a la venta.
     *
     * @param compradorId identificador numerico del comprador.
     */
    public void setCompradorId(Integer compradorId) {
        this.compradorId = compradorId;
    }

    /**
     * Obtiene el identificador de operacion distribuida (trazabilidad Actividad 10 - P27).
     *
     * @return operacionId compartido entre reserva, venta y notificacion.
     */
    public String getOperacionId() {
        return operacionId;
    }

    /**
     * Establece el identificador de operacion distribuida.
     *
     * @param operacionId clave de correlacion de la operacion.
     */
    public void setOperacionId(String operacionId) {
        this.operacionId = operacionId;
    }

    /**
     * Obtiene el texto descriptivo de la notificacion.
     *
     * @return mensaje legible.
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Establece el texto descriptivo de la notificacion.
     *
     * @param mensaje contenido informativo del aviso.
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    /**
     * Obtiene el estado de procesamiento del registro.
     *
     * @return estado (PROCESADA o ENVIADA).
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado de procesamiento del registro.
     *
     * @param estado etiqueta de estado.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene la fecha y hora de registro formateada.
     *
     * @return marca temporal en formato yyyy-MM-dd HH:mm:ss.
     */
    public String getFechaRegistro() {
        return fechaRegistro;
    }

    /**
     * Establece la fecha y hora de registro formateada.
     *
     * @param fechaRegistro marca temporal textual.
     */
    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
