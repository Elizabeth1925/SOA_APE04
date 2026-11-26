/**
 * ==============================================================================
 * ARCHIVO: EstadisticasResponse.java
 * COMPONENTE: monitor-balancer-service (DTO de Estadisticas Cuantitativas HTTP)
 * ROL: Modela la respuesta JSON de GET /api/monitor/estadisticas con el conteo
 *      de respuestas HTTP 2xx, 4xx y 5xx, el tiempo promedio de respuesta en
 *      milisegundos y la descripcion del criterio temporal aplicado.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P24/P25/P26]: Permite verificar tasa de
 *     exito 2xx, ausencia de errores 5xx durante failover pasivo y latencia
 *     promedio exclusivamente sobre la ventana de prueba activa.
 * ==============================================================================
 */
// Declara el paquete de DTOs del servicio de monitoreo.
package ec.edu.uta.monitor_balancer_service.dto;

/**
 * DTO de salida con las metricas agregadas de codigos de estado HTTP y latencia.
 */
public class EstadisticasResponse {

    // Timestamp ISO-8601 de la primera peticion incluida en la ventana analizada.
    private String inicioVentana;
    // Timestamp ISO-8601 de la ultima peticion incluida en la ventana analizada.
    private String finVentana;
    // Total de peticiones HTTP contabilizadas en la ventana (excluyendo /nginx-ping).
    private long totalPeticiones;
    // Total de respuestas exitosas con codigo HTTP entre 200 y 299.
    private long total2xx;
    // Total de respuestas con error de cliente HTTP entre 400 y 499.
    private long total4xx;
    // Total de respuestas con error de servidor/gateway HTTP entre 500 y 599.
    private long total5xx;
    // Tiempo promedio total de atencion reportado por NGINX en milisegundos (redondeado a 2 decimales).
    private double promedioRespuestaMs;
    // Descripcion legible de la regla de ventana aplicada (por ejemplo "Ventana de ultimos 30 minutos" o "Ventana explicita desde ...").
    private String criterioVentana;

    /**
     * Constructor vacio requerido por Jackson.
     */
    public EstadisticasResponse() {
    }

    /**
     * Constructor completo para construir el resumen estadistico de una ventana.
     *
     * @param inicioVentana       marca temporal del primer evento en la ventana.
     * @param finVentana          marca temporal del ultimo evento en la ventana.
     * @param totalPeticiones     numero total de peticiones analizadas.
     * @param total2xx            cantidad de respuestas 2xx.
     * @param total4xx            cantidad de respuestas 4xx.
     * @param total5xx            cantidad de respuestas 5xx.
     * @param promedioRespuestaMs promedio de tiempo de respuesta en ms.
     * @param criterioVentana     descripcion del filtro temporal aplicado.
     */
    public EstadisticasResponse(String inicioVentana, String finVentana, long totalPeticiones,
                                long total2xx, long total4xx, long total5xx,
                                double promedioRespuestaMs, String criterioVentana) {
        // Asigna el timestamp inicial de los registros.
        this.inicioVentana = inicioVentana;
        // Asigna el timestamp final de los registros.
        this.finVentana = finVentana;
        // Asigna el total de peticiones medidas.
        this.totalPeticiones = totalPeticiones;
        // Asigna el conteo de respuestas 2xx.
        this.total2xx = total2xx;
        // Asigna el conteo de respuestas 4xx.
        this.total4xx = total4xx;
        // Asigna el conteo de respuestas 5xx.
        this.total5xx = total5xx;
        // Asigna la latencia promedio en milisegundos.
        this.promedioRespuestaMs = promedioRespuestaMs;
        // Asigna la etiqueta descriptiva del criterio de ventana temporal.
        this.criterioVentana = criterioVentana;
    }

    /**
     * Obtiene el timestamp del primer registro en la ventana.
     *
     * @return inicioVentana o null si no hubo datos.
     */
    public String getInicioVentana() {
        return inicioVentana;
    }

    /**
     * Establece el timestamp del primer registro en la ventana.
     *
     * @param inicioVentana marca temporal inicial.
     */
    public void setInicioVentana(String inicioVentana) {
        this.inicioVentana = inicioVentana;
    }

    /**
     * Obtiene el timestamp del ultimo registro en la ventana.
     *
     * @return finVentana o null si no hubo datos.
     */
    public String getFinVentana() {
        return finVentana;
    }

    /**
     * Establece el timestamp del ultimo registro en la ventana.
     *
     * @param finVentana marca temporal final.
     */
    public void setFinVentana(String finVentana) {
        this.finVentana = finVentana;
    }

    /**
     * Obtiene el numero total de peticiones evaluadas.
     *
     * @return total de peticiones.
     */
    public long getTotalPeticiones() {
        return totalPeticiones;
    }

    /**
     * Establece el numero total de peticiones evaluadas.
     *
     * @param totalPeticiones conteo total.
     */
    public void setTotalPeticiones(long totalPeticiones) {
        this.totalPeticiones = totalPeticiones;
    }

    /**
     * Obtiene el total de respuestas HTTP 2xx.
     *
     * @return conteo de codigos 200-299.
     */
    public long getTotal2xx() {
        return total2xx;
    }

    /**
     * Establece el total de respuestas HTTP 2xx.
     *
     * @param total2xx conteo de exitos.
     */
    public void setTotal2xx(long total2xx) {
        this.total2xx = total2xx;
    }

    /**
     * Obtiene el total de respuestas HTTP 4xx.
     *
     * @return conteo de errores de cliente 400-499.
     */
    public long getTotal4xx() {
        return total4xx;
    }

    /**
     * Establece el total de respuestas HTTP 4xx.
     *
     * @param total4xx conteo de codigos 4xx.
     */
    public void setTotal4xx(long total4xx) {
        this.total4xx = total4xx;
    }

    /**
     * Obtiene el total de respuestas HTTP 5xx.
     *
     * @return conteo de errores de servidor 500-599.
     */
    public long getTotal5xx() {
        return total5xx;
    }

    /**
     * Establece el total de respuestas HTTP 5xx.
     *
     * @param total5xx conteo de codigos 5xx.
     */
    public void setTotal5xx(long total5xx) {
        this.total5xx = total5xx;
    }

    /**
     * Obtiene el promedio de tiempo de respuesta en milisegundos.
     *
     * @return latencia promedio en ms.
     */
    public double getPromedioRespuestaMs() {
        return promedioRespuestaMs;
    }

    /**
     * Establece el promedio de tiempo de respuesta en milisegundos.
     *
     * @param promedioRespuestaMs latencia promedio en ms.
     */
    public void setPromedioRespuestaMs(double promedioRespuestaMs) {
        this.promedioRespuestaMs = promedioRespuestaMs;
    }

    /**
     * Obtiene la descripcion textual del filtro de ventana temporal utilizado.
     *
     * @return criterio de ventana aplicado.
     */
    public String getCriterioVentana() {
        return criterioVentana;
    }

    /**
     * Establece la descripcion textual del filtro de ventana temporal utilizado.
     *
     * @param criterioVentana cadena descriptiva de la ventana.
     */
    public void setCriterioVentana(String criterioVentana) {
        this.criterioVentana = criterioVentana;
    }
}
