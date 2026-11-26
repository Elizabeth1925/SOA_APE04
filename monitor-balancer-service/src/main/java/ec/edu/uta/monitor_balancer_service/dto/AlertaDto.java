/**
 * ==============================================================================
 * ARCHIVO: AlertaDto.java
 * COMPONENTE: monitor-balancer-service (DTO de Alertas de Transicion)
 * ROL: Modela cada evento de cambio de estado detectado por MonitorService
 *      (por ejemplo UP -> DOWN o DEGRADADO -> NORMAL) expuesto en
 *      GET /api/monitor/alertas.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P25/P26]: Evidencia auditable de caidas de
 *     instancias (P25) y recuperaciones automaticas (P26) sin alertas repetidas.
 * ==============================================================================
 */
// Declara el paquete de DTOs de respuesta del servicio de monitoreo.
package ec.edu.uta.monitor_balancer_service.dto;

/**
 * DTO que representa una alerta generada cuando un componente o el estado
 * general cambia respecto al sondeo previo.
 */
public class AlertaDto {

    // Marca temporal ISO-8601 con zona horaria del sondeo en que se detecto la transicion.
    private String timestamp;
    // Nombre del componente afectado: NGINX, Vehiculos_8080, Vehiculos_8082 o ESTADO_GENERAL.
    private String componente;
    // Estado en el que se encontraba el componente en el ciclo de sondeo anterior.
    private String estadoAnterior;
    // Nuevo estado observado que disparo la generacion de la alerta.
    private String estadoNuevo;
    // Mensaje legible resumiendo la transicion detectada para auditoria y visualizacion.
    private String mensaje;

    /**
     * Constructor vacio requerido por Jackson para serializacion y deserializacion JSON.
     */
    public AlertaDto() {
    }

    /**
     * Constructor completo para registrar una transicion de estado.
     *
     * @param timestamp      marca temporal ISO-8601 del incidente.
     * @param componente     identificador del componente monitoreado.
     * @param estadoAnterior estado inmediatamente anterior.
     * @param estadoNuevo    estado actual tras la transicion.
     * @param mensaje        descripcion textual de la alerta.
     */
    public AlertaDto(String timestamp, String componente, String estadoAnterior, String estadoNuevo, String mensaje) {
        // Asigna la marca temporal del evento.
        this.timestamp = timestamp;
        // Asigna el nombre del componente afectado.
        this.componente = componente;
        // Asigna el estado anterior.
        this.estadoAnterior = estadoAnterior;
        // Asigna el nuevo estado detectado.
        this.estadoNuevo = estadoNuevo;
        // Asigna el mensaje descriptivo.
        this.mensaje = mensaje;
    }

    /**
     * Obtiene la marca temporal ISO-8601 de la alerta.
     *
     * @return timestamp de deteccion.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Establece la marca temporal ISO-8601 de la alerta.
     *
     * @param timestamp cadena temporal ISO-8601.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * Obtiene el componente que experimento la transicion.
     *
     * @return nombre del componente.
     */
    public String getComponente() {
        return componente;
    }

    /**
     * Establece el componente que experimento la transicion.
     *
     * @param componente nombre del componente.
     */
    public void setComponente(String componente) {
        this.componente = componente;
    }

    /**
     * Obtiene el estado previo antes de la transicion.
     *
     * @return estado anterior.
     */
    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    /**
     * Establece el estado previo antes de la transicion.
     *
     * @param estadoAnterior estado anterior.
     */
    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    /**
     * Obtiene el estado nuevo observado tras la transicion.
     *
     * @return estado nuevo.
     */
    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    /**
     * Establece el estado nuevo observado tras la transicion.
     *
     * @param estadoNuevo estado nuevo.
     */
    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    /**
     * Obtiene el mensaje descriptivo de la alerta.
     *
     * @return texto resumen de la transicion.
     */
    public String getMensaje() {
        return mensaje;
    }

    /**
     * Establece el mensaje descriptivo de la alerta.
     *
     * @param mensaje texto resumen de la transicion.
     */
    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
