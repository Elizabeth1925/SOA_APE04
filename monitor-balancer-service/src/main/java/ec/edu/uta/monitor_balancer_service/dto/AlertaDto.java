package ec.edu.uta.monitor_balancer_service.dto;

public class AlertaDto {

    private String timestamp;
    private String componente;
    private String estadoAnterior;
    private String estadoNuevo;
    private String mensaje;

    public AlertaDto() {
    }

    public AlertaDto(String timestamp, String componente, String estadoAnterior, String estadoNuevo, String mensaje) {
        this.timestamp = timestamp;
        this.componente = componente;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.mensaje = mensaje;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getComponente() {
        return componente;
    }

    public void setComponente(String componente) {
        this.componente = componente;
    }

    public String getEstadoAnterior() {
        return estadoAnterior;
    }

    public void setEstadoAnterior(String estadoAnterior) {
        this.estadoAnterior = estadoAnterior;
    }

    public String getEstadoNuevo() {
        return estadoNuevo;
    }

    public void setEstadoNuevo(String estadoNuevo) {
        this.estadoNuevo = estadoNuevo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
