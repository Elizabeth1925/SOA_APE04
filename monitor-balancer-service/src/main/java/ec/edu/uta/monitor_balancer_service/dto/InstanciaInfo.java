/**
 * ==============================================================================
 * ARCHIVO: InstanciaInfo.java
 * COMPONENTE: monitor-balancer-service (DTO de Salud por Instancia Backend)
 * ROL: Representa el estado individual y la latencia medida de cada instancia
 *      Spring Boot de Vehiculos (:8080 y :8082) en GET /api/monitor/instancias
 *      y dentro de GET /api/monitor/estado.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P23/P25/P26]: Incluye explicitamente los
 *     campos nulos (@JsonInclude ALWAYS) para evidenciar "latenciaMs": null
 *     cuando una instancia se encuentra caida (DOWN).
 * ==============================================================================
 */
// Declara el paquete de DTOs de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.dto;

// Importa JsonInclude de Jackson para controlar la serializacion de propiedades con valor null.
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO que describe la salud observada de una instancia backend concreta.
 */
// Fuerza a Jackson a serializar siempre todos los campos, manteniendo "latenciaMs": null visible cuando la instancia esta DOWN.
@JsonInclude(JsonInclude.Include.ALWAYS)
public class InstanciaInfo {

    // Puerto TCP de la instancia monitoreada (8080 para Instancia A, 8082 para Instancia B).
    private int puerto;
    // URL completa del endpoint Actuator sondeado (ej. http://127.0.0.1:8080/actuator/health).
    private String url;
    // Estado resultante del ultimo sondeo: UP, DOWN o ERROR_ACCESO.
    private String estado; // UP, DOWN, ERROR_ACCESO
    // Latencia medida en milisegundos durante el sondeo, o null si no fue posible conectar (DOWN).
    private Long latenciaMs; // null si DOWN
    // Marca temporal ISO-8601 del ultimo sondeo realizado sobre esta instancia.
    private String ultimaRevision;

    /**
     * Constructor vacio requerido por Jackson.
     */
    public InstanciaInfo() {
    }

    /**
     * Constructor completo para describir el estado de una instancia.
     *
     * @param puerto         puerto TCP de la instancia backend.
     * @param url            endpoint de salud consultado.
     * @param estado         resultado de salud (UP, DOWN o ERROR_ACCESO).
     * @param latenciaMs     latencia medida en ms o null si la instancia no respondio.
     * @param ultimaRevision marca temporal ISO-8601 del sondeo.
     */
    public InstanciaInfo(int puerto, String url, String estado, Long latenciaMs, String ultimaRevision) {
        // Asigna el puerto de la instancia.
        this.puerto = puerto;
        // Asigna la URL de salud sondeada.
        this.url = url;
        // Asigna el estado de salud actual.
        this.estado = estado;
        // Asigna la latencia medida (o null si esta DOWN).
        this.latenciaMs = latenciaMs;
        // Asigna el timestamp de la ultima revision.
        this.ultimaRevision = ultimaRevision;
    }

    /**
     * Obtiene el puerto TCP de la instancia.
     *
     * @return numero de puerto (8080 u 8082).
     */
    public int getPuerto() {
        return puerto;
    }

    /**
     * Establece el puerto TCP de la instancia.
     *
     * @param puerto numero de puerto.
     */
    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    /**
     * Obtiene la URL del endpoint de salud sondeado.
     *
     * @return URL de Actuator.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Establece la URL del endpoint de salud sondeado.
     *
     * @param url URL de Actuator.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Obtiene el estado de salud actual de la instancia.
     *
     * @return UP, DOWN o ERROR_ACCESO.
     */
    public String getEstado() {
        return estado;
    }

    /**
     * Establece el estado de salud actual de la instancia.
     *
     * @param estado etiqueta de salud.
     */
    public void setEstado(String estado) {
        this.estado = estado;
    }

    /**
     * Obtiene la latencia del ultimo sondeo en milisegundos.
     *
     * @return latencia en ms o null si el nodo fallo.
     */
    public Long getLatenciaMs() {
        return latenciaMs;
    }

    /**
     * Establece la latencia del ultimo sondeo en milisegundos.
     *
     * @param latenciaMs tiempo de respuesta en ms o null.
     */
    public void setLatenciaMs(Long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    /**
     * Obtiene la marca temporal ISO-8601 del ultimo sondeo.
     *
     * @return timestamp de ultima revision.
     */
    public String getUltimaRevision() {
        return ultimaRevision;
    }

    /**
     * Establece la marca temporal ISO-8601 del ultimo sondeo.
     *
     * @param ultimaRevision timestamp del sondeo.
     */
    public void setUltimaRevision(String ultimaRevision) {
        this.ultimaRevision = ultimaRevision;
    }
}
