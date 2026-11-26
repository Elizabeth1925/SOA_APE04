/**
 * ==============================================================================
 * ARCHIVO: EstadoGeneralResponse.java
 * COMPONENTE: monitor-balancer-service (DTO de Estado Agregado del Sistema)
 * ROL: Modela la instantanea consolidada devuelta por GET /api/monitor/estado,
 *      incluyendo el estado de NGINX, el estado agregado del cluster de
 *      Vehiculos, el detalle de ambas instancias y la hora del ultimo sondeo.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P23/P25/P26]: Expone el diagnostico global
 *     NORMAL (2/2 UP), DEGRADADO (1/2 UP), SIN_BACKENDS (0/2 UP) o NGINX_DOWN.
 * ==============================================================================
 */
// Declara el paquete de DTOs de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.dto;

// Importa List para contener el listado de instancias monitoreadas (8080 y 8082).
import java.util.List;

/**
 * DTO principal que resume la salud global del balanceador NGINX y sus nodos backend.
 */
public class EstadoGeneralResponse {

    // Estado del proxy inverso NGINX sondeado en /nginx-ping (UP o DOWN).
    private String nginx; // UP, DOWN
    // Estado agregado calculado segun la disponibilidad de NGINX y los backends (NORMAL, DEGRADADO, SIN_BACKENDS, NGINX_DOWN).
    private String estadoGeneral; // NORMAL, DEGRADADO, SIN_BACKENDS, NGINX_DOWN
    // Lista con el estado individual y latencia de la Instancia A (:8080) y la Instancia B (:8082).
    private List<InstanciaInfo> instancias;
    // Marca temporal ISO-8601 con zona horaria correspondiente al ultimo sondeo periodico ejecutado.
    private String ultimaRevision;

    /**
     * Constructor vacio requerido por Jackson.
     */
    public EstadoGeneralResponse() {
    }

    /**
     * Constructor completo de la instantanea de estado general.
     *
     * @param nginx          estado actual de NGINX (UP o DOWN).
     * @param estadoGeneral  estado agregado del sistema.
     * @param instancias     detalle individual de las instancias de Vehiculos.
     * @param ultimaRevision timestamp ISO-8601 del ultimo sondeo.
     */
    public EstadoGeneralResponse(String nginx, String estadoGeneral, List<InstanciaInfo> instancias, String ultimaRevision) {
        // Asigna el estado de NGINX.
        this.nginx = nginx;
        // Asigna la clasificacion de estado agregado.
        this.estadoGeneral = estadoGeneral;
        // Asigna la lista de instancias monitoreadas.
        this.instancias = instancias;
        // Asigna la marca temporal del sondeo.
        this.ultimaRevision = ultimaRevision;
    }

    /**
     * Obtiene el estado reportado de NGINX.
     *
     * @return UP o DOWN.
     */
    public String getNginx() {
        return nginx;
    }

    /**
     * Establece el estado reportado de NGINX.
     *
     * @param nginx estado de NGINX.
     */
    public void setNginx(String nginx) {
        this.nginx = nginx;
    }

    /**
     * Obtiene el estado agregado general del sistema.
     *
     * @return NORMAL, DEGRADADO, SIN_BACKENDS o NGINX_DOWN.
     */
    public String getEstadoGeneral() {
        return estadoGeneral;
    }

    /**
     * Establece el estado agregado general del sistema.
     *
     * @param estadoGeneral clasificacion agregada.
     */
    public void setEstadoGeneral(String estadoGeneral) {
        this.estadoGeneral = estadoGeneral;
    }

    /**
     * Obtiene la lista de instancias monitoreadas con su estado y latencia.
     *
     * @return lista de InstanciaInfo (puertos 8080 y 8082).
     */
    public List<InstanciaInfo> getInstancias() {
        return instancias;
    }

    /**
     * Establece la lista de instancias monitoreadas.
     *
     * @param instancias coleccion de InstanciaInfo.
     */
    public void setInstancias(List<InstanciaInfo> instancias) {
        this.instancias = instancias;
    }

    /**
     * Obtiene la marca temporal ISO-8601 de la ultima revision ejecutada.
     *
     * @return timestamp de ultima revision.
     */
    public String getUltimaRevision() {
        return ultimaRevision;
    }

    /**
     * Establece la marca temporal ISO-8601 de la ultima revision ejecutada.
     *
     * @param ultimaRevision timestamp del ultimo sondeo.
     */
    public void setUltimaRevision(String ultimaRevision) {
        this.ultimaRevision = ultimaRevision;
    }
}
