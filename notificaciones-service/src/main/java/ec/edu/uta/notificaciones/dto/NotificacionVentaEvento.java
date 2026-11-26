/**
 * ==============================================================================
 * ARCHIVO: NotificacionVentaEvento.java
 * COMPONENTE: notificaciones-service (DTO de Contrato de Evento AMQP)
 * ROL: Representa el esquema JSON del evento de venta publicado por ventas-api
 *      (FastAPI) en el exchange de RabbitMQ y consumido desde notificaciones.ventas.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Contrato canonico del mensaje asincrono AMQP
 *     deserializado por VentasRabbitListener.
 *   - [Actividad 10 - P27]: Contiene ventaId y operacionId para mantener la
 *     trazabilidad completa desde la venta hasta la vista de JavaFX.
 *   - [Actividad 11 - P22]: Expone eventoId como clave primaria de idempotencia
 *     para detectar y descartar entregas duplicadas en RabbitMQ.
 * ==============================================================================
 */
// Declara el paquete de DTOs del servicio de notificaciones.
package ec.edu.uta.notificaciones.dto;

// Importa la interfaz marcadora Serializable para compatibilidad con transporte de mensajes en Java.
import java.io.Serializable;

/**
 * DTO que mapea el cuerpo JSON de un evento de venta consumido desde RabbitMQ.
 */
public class NotificacionVentaEvento implements Serializable {
    // [Actividad 11 - P22] Identificador unico del evento usado para garantizar deduplicacion estricta.
    private String eventoId;
    // Tipo de evento emitido por el productor (habitualmente "VENTA_CONFIRMADA" o "VENTA").
    private String tipo;
    // [Actividad 10 - P27] Identificador unico de la venta registrada en ventas-api.
    private Integer ventaId;
    // Identificador del vehiculo vendido en el catalogo de Spring Boot.
    private Integer vehiculoId;
    // Identificador del comprador registrado en ventas-api.
    private Integer compradorId;
    // [Actividad 10 - P27] Identificador de correlacion distribuida (operacionId) que une reserva y venta.
    private String operacionId;
    // Marca temporal UTC ISO-8601 en la que el productor genero el evento de venta.
    private String fechaUTC;

    /**
     * Constructor vacio requerido por Jackson para instanciar el DTO al deserializar mensajes JSON.
     */
    public NotificacionVentaEvento() {
    }

    /**
     * Constructor completo para crear un evento de venta con todos sus campos de contrato.
     *
     * @param eventoId    identificador unico del evento (idempotencia).
     * @param tipo        tipo de evento de negocio.
     * @param ventaId     identificador de la venta confirmada.
     * @param vehiculoId  identificador del vehiculo vendido.
     * @param compradorId identificador del comprador.
     * @param operacionId clave de correlacion distribuida de la operacion.
     * @param fechaUTC    fecha y hora en UTC generada por el emisor.
     */
    public NotificacionVentaEvento(String eventoId, String tipo, Integer ventaId, Integer vehiculoId, Integer compradorId, String operacionId, String fechaUTC) {
        // Asigna la clave unica de idempotencia del evento.
        this.eventoId = eventoId;
        // Asigna el tipo de evento de negocio.
        this.tipo = tipo;
        // Asigna el identificador de la venta.
        this.ventaId = ventaId;
        // Asigna el identificador del vehiculo.
        this.vehiculoId = vehiculoId;
        // Asigna el identificador del comprador.
        this.compradorId = compradorId;
        // Asigna el identificador de correlacion de la operacion.
        this.operacionId = operacionId;
        // Asigna la fecha UTC de emision del evento.
        this.fechaUTC = fechaUTC;
    }

    /**
     * Obtiene el identificador unico del evento (clave de deduplicacion P22).
     *
     * @return eventoId.
     */
    public String getEventoId() {
        return eventoId;
    }

    /**
     * Establece el identificador unico del evento.
     *
     * @param eventoId cadena unica de evento.
     */
    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    /**
     * Obtiene el tipo de evento reportado por el productor.
     *
     * @return tipo de evento.
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Establece el tipo de evento reportado por el productor.
     *
     * @param tipo cadena de tipo de evento.
     */
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    /**
     * Obtiene el identificador de la venta confirmada (trazabilidad P27).
     *
     * @return ventaId.
     */
    public Integer getVentaId() {
        return ventaId;
    }

    /**
     * Establece el identificador de la venta confirmada.
     *
     * @param ventaId identificador de la venta.
     */
    public void setVentaId(Integer ventaId) {
        this.ventaId = ventaId;
    }

    /**
     * Obtiene el identificador del vehiculo involucrado en la venta.
     *
     * @return vehiculoId.
     */
    public Integer getVehiculoId() {
        return vehiculoId;
    }

    /**
     * Establece el identificador del vehiculo involucrado en la venta.
     *
     * @param vehiculoId identificador del vehiculo.
     */
    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    /**
     * Obtiene el identificador del comprador asociado a la venta.
     *
     * @return compradorId.
     */
    public Integer getCompradorId() {
        return compradorId;
    }

    /**
     * Establece el identificador del comprador asociado a la venta.
     *
     * @param compradorId identificador del comprador.
     */
    public void setCompradorId(Integer compradorId) {
        this.compradorId = compradorId;
    }

    /**
     * Obtiene el identificador de operacion distribuida (trazabilidad P27).
     *
     * @return operacionId compartido entre los microservicios.
     */
    public String getOperacionId() {
        return operacionId;
    }

    /**
     * Establece el identificador de operacion distribuida.
     *
     * @param operacionId identificador de correlacion de la operacion.
     */
    public void setOperacionId(String operacionId) {
        this.operacionId = operacionId;
    }

    /**
     * Obtiene la fecha y hora UTC en que se emitio el evento.
     *
     * @return cadena de fecha en formato UTC.
     */
    public String getFechaUTC() {
        return fechaUTC;
    }

    /**
     * Establece la fecha y hora UTC en que se emitio el evento.
     *
     * @param fechaUTC marca temporal en formato UTC.
     */
    public void setFechaUTC(String fechaUTC) {
        this.fechaUTC = fechaUTC;
    }
}
