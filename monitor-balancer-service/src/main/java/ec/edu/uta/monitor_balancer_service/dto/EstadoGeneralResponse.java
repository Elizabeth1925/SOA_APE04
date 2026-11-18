package ec.edu.uta.monitor_balancer_service.dto;

import java.util.List;

public class EstadoGeneralResponse {

    private String nginx; // UP, DOWN
    private String estadoGeneral; // NORMAL, DEGRADADO, SIN_BACKENDS, NGINX_DOWN
    private List<InstanciaInfo> instancias;
    private String ultimaRevision;

    public EstadoGeneralResponse() {
    }

    public EstadoGeneralResponse(String nginx, String estadoGeneral, List<InstanciaInfo> instancias, String ultimaRevision) {
        this.nginx = nginx;
        this.estadoGeneral = estadoGeneral;
        this.instancias = instancias;
        this.ultimaRevision = ultimaRevision;
    }

    public String getNginx() {
        return nginx;
    }

    public void setNginx(String nginx) {
        this.nginx = nginx;
    }

    public String getEstadoGeneral() {
        return estadoGeneral;
    }

    public void setEstadoGeneral(String estadoGeneral) {
        this.estadoGeneral = estadoGeneral;
    }

    public List<InstanciaInfo> getInstancias() {
        return instancias;
    }

    public void setInstancias(List<InstanciaInfo> instancias) {
        this.instancias = instancias;
    }

    public String getUltimaRevision() {
        return ultimaRevision;
    }

    public void setUltimaRevision(String ultimaRevision) {
        this.ultimaRevision = ultimaRevision;
    }
}
