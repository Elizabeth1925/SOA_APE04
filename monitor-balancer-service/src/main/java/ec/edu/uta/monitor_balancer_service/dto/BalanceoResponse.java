/**
 * ==============================================================================
 * ARCHIVO: BalanceoResponse.java
 * COMPONENTE: monitor-balancer-service (DTO de Metricas de Balanceo)
 * ROL: Modela la respuesta JSON de GET /api/monitor/balanceo con la distribucion
 *      real de trafico atendido por las instancias A (:8080) y B (:8082) dentro
 *      de la ventana temporal consultada en access.log.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P24/P25]: Cuantifica el balanceo ponderado
 *     3:1 (~75% en 8080 y ~25% en 8082 en P24) y evidencia el redireccionamiento
 *     hacia el nodo sano durante la prueba de failover (P25).
 * ==============================================================================
 */
// Declara el paquete de DTOs de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.dto;

/**
 * DTO de salida con el conteo absoluto y porcentual del trafico distribuido por NGINX.
 */
public class BalanceoResponse {

    // Timestamp ISO-8601 de la primera solicitud encontrada dentro de la ventana temporal.
    private String inicioVentana;
    // Timestamp ISO-8601 de la ultima solicitud encontrada dentro de la ventana temporal.
    private String finVentana;
    // Cantidad total de solicitudes analizadas dentro de la ventana (excluyendo /nginx-ping).
    private long totalSolicitudes;
    // Cantidad de solicitudes atendidas finalmente por la Instancia A (puerto 8080, weight=3).
    private long solicitudesA;
    // Cantidad de solicitudes atendidas finalmente por la Instancia B (puerto 8082, weight=1).
    private long solicitudesB;
    // Cantidad de solicitudes sin upstream de vehiculos (ej. rutas a FastAPI :8002, Notificaciones :8081 o respuestas locales).
    private long sinUpstream;
    // Porcentaje redondeado a 2 decimales de trafico atendido por la Instancia A respecto al total.
    private double porcentajeA;
    // Porcentaje redondeado a 2 decimales de trafico atendido por la Instancia B respecto al total.
    private double porcentajeB;

    /**
     * Constructor por defecto requerido por Jackson.
     */
    public BalanceoResponse() {
    }

    /**
     * Constructor completo con todas las metricas de balanceo calculadas.
     *
     * @param inicioVentana    marca temporal del primer registro en la ventana.
     * @param finVentana       marca temporal del ultimo registro en la ventana.
     * @param totalSolicitudes numero total de registros analizados.
     * @param solicitudesA     conteo de peticiones atendidas por la instancia A (8080).
     * @param solicitudesB     conteo de peticiones atendidas por la instancia B (8082).
     * @param sinUpstream      conteo de peticiones que no correspondieron a 8080 ni 8082.
     * @param porcentajeA      porcentaje asignado a la instancia A.
     * @param porcentajeB      porcentaje asignado a la instancia B.
     */
    public BalanceoResponse(String inicioVentana, String finVentana, long totalSolicitudes,
                            long solicitudesA, long solicitudesB, long sinUpstream,
                            double porcentajeA, double porcentajeB) {
        // Asigna el timestamp inicial de la muestra.
        this.inicioVentana = inicioVentana;
        // Asigna el timestamp final de la muestra.
        this.finVentana = finVentana;
        // Asigna el total de solicitudes evaluadas.
        this.totalSolicitudes = totalSolicitudes;
        // Asigna el total atendido por la instancia A (:8080).
        this.solicitudesA = solicitudesA;
        // Asigna el total atendido por la instancia B (:8082).
        this.solicitudesB = solicitudesB;
        // Asigna el total sin correspondencia con el upstream de vehiculos.
        this.sinUpstream = sinUpstream;
        // Asigna el porcentaje de la instancia A.
        this.porcentajeA = porcentajeA;
        // Asigna el porcentaje de la instancia B.
        this.porcentajeB = porcentajeB;
    }

    /**
     * Obtiene la marca temporal del primer registro de la ventana.
     *
     * @return timestamp ISO-8601 inicial o null si no hubo registros.
     */
    public String getInicioVentana() {
        return inicioVentana;
    }

    /**
     * Establece la marca temporal del primer registro de la ventana.
     *
     * @param inicioVentana timestamp inicial.
     */
    public void setInicioVentana(String inicioVentana) {
        this.inicioVentana = inicioVentana;
    }

    /**
     * Obtiene la marca temporal del ultimo registro de la ventana.
     *
     * @return timestamp ISO-8601 final o null si no hubo registros.
     */
    public String getFinVentana() {
        return finVentana;
    }

    /**
     * Establece la marca temporal del ultimo registro de la ventana.
     *
     * @param finVentana timestamp final.
     */
    public void setFinVentana(String finVentana) {
        this.finVentana = finVentana;
    }

    /**
     * Obtiene el total de solicitudes procesadas en la ventana.
     *
     * @return total de solicitudes.
     */
    public long getTotalSolicitudes() {
        return totalSolicitudes;
    }

    /**
     * Establece el total de solicitudes procesadas en la ventana.
     *
     * @param totalSolicitudes numero total de registros.
     */
    public void setTotalSolicitudes(long totalSolicitudes) {
        this.totalSolicitudes = totalSolicitudes;
    }

    /**
     * Obtiene el conteo de solicitudes atendidas por la instancia A (puerto 8080).
     *
     * @return conteo de la instancia A.
     */
    public long getSolicitudesA() {
        return solicitudesA;
    }

    /**
     * Establece el conteo de solicitudes atendidas por la instancia A.
     *
     * @param solicitudesA conteo para :8080.
     */
    public void setSolicitudesA(long solicitudesA) {
        this.solicitudesA = solicitudesA;
    }

    /**
     * Obtiene el conteo de solicitudes atendidas por la instancia B (puerto 8082).
     *
     * @return conteo de la instancia B.
     */
    public long getSolicitudesB() {
        return solicitudesB;
    }

    /**
     * Establece el conteo de solicitudes atendidas por la instancia B.
     *
     * @param solicitudesB conteo para :8082.
     */
    public void setSolicitudesB(long solicitudesB) {
        this.solicitudesB = solicitudesB;
    }

    /**
     * Obtiene el conteo de solicitudes que no corresponden al upstream :8080/:8082.
     *
     * @return conteo sin upstream de vehiculos.
     */
    public long getSinUpstream() {
        return sinUpstream;
    }

    /**
     * Establece el conteo de solicitudes sin upstream de vehiculos.
     *
     * @param sinUpstream conteo de otras rutas o fallos sin backend.
     */
    public void setSinUpstream(long sinUpstream) {
        this.sinUpstream = sinUpstream;
    }

    /**
     * Obtiene el porcentaje de solicitudes atendidas por la instancia A.
     *
     * @return porcentaje (0.0 a 100.0).
     */
    public double getPorcentajeA() {
        return porcentajeA;
    }

    /**
     * Establece el porcentaje de solicitudes atendidas por la instancia A.
     *
     * @param porcentajeA porcentaje de :8080.
     */
    public void setPorcentajeA(double porcentajeA) {
        this.porcentajeA = porcentajeA;
    }

    /**
     * Obtiene el porcentaje de solicitudes atendidas por la instancia B.
     *
     * @return porcentaje (0.0 a 100.0).
     */
    public double getPorcentajeB() {
        return porcentajeB;
    }

    /**
     * Establece el porcentaje de solicitudes atendidas por la instancia B.
     *
     * @param porcentajeB porcentaje de :8082.
     */
    public void setPorcentajeB(double porcentajeB) {
        this.porcentajeB = porcentajeB;
    }
}
