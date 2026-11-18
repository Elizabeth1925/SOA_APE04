package ec.edu.uta.monitor_balancer_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class InstanciaInfo {

    private int puerto;
    private String url;
    private String estado; // UP, DOWN, ERROR_ACCESO
    private Long latenciaMs; // null si DOWN
    private String ultimaRevision;

    public InstanciaInfo() {
    }

    public InstanciaInfo(int puerto, String url, String estado, Long latenciaMs, String ultimaRevision) {
        this.puerto = puerto;
        this.url = url;
        this.estado = estado;
        this.latenciaMs = latenciaMs;
        this.ultimaRevision = ultimaRevision;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Long getLatenciaMs() {
        return latenciaMs;
    }

    public void setLatenciaMs(Long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    public String getUltimaRevision() {
        return ultimaRevision;
    }

    public void setUltimaRevision(String ultimaRevision) {
        this.ultimaRevision = ultimaRevision;
    }
}
