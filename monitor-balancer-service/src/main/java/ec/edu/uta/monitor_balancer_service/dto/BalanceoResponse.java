package ec.edu.uta.monitor_balancer_service.dto;

public class BalanceoResponse {

    private String inicioVentana;
    private String finVentana;
    private long totalSolicitudes;
    private long solicitudesA;
    private long solicitudesB;
    private long sinUpstream;
    private double porcentajeA;
    private double porcentajeB;

    public BalanceoResponse() {
    }

    public BalanceoResponse(String inicioVentana, String finVentana, long totalSolicitudes,
                            long solicitudesA, long solicitudesB, long sinUpstream,
                            double porcentajeA, double porcentajeB) {
        this.inicioVentana = inicioVentana;
        this.finVentana = finVentana;
        this.totalSolicitudes = totalSolicitudes;
        this.solicitudesA = solicitudesA;
        this.solicitudesB = solicitudesB;
        this.sinUpstream = sinUpstream;
        this.porcentajeA = porcentajeA;
        this.porcentajeB = porcentajeB;
    }

    public String getInicioVentana() {
        return inicioVentana;
    }

    public void setInicioVentana(String inicioVentana) {
        this.inicioVentana = inicioVentana;
    }

    public String getFinVentana() {
        return finVentana;
    }

    public void setFinVentana(String finVentana) {
        this.finVentana = finVentana;
    }

    public long getTotalSolicitudes() {
        return totalSolicitudes;
    }

    public void setTotalSolicitudes(long totalSolicitudes) {
        this.totalSolicitudes = totalSolicitudes;
    }

    public long getSolicitudesA() {
        return solicitudesA;
    }

    public void setSolicitudesA(long solicitudesA) {
        this.solicitudesA = solicitudesA;
    }

    public long getSolicitudesB() {
        return solicitudesB;
    }

    public void setSolicitudesB(long solicitudesB) {
        this.solicitudesB = solicitudesB;
    }

    public long getSinUpstream() {
        return sinUpstream;
    }

    public void setSinUpstream(long sinUpstream) {
        this.sinUpstream = sinUpstream;
    }

    public double getPorcentajeA() {
        return porcentajeA;
    }

    public void setPorcentajeA(double porcentajeA) {
        this.porcentajeA = porcentajeA;
    }

    public double getPorcentajeB() {
        return porcentajeB;
    }

    public void setPorcentajeB(double porcentajeB) {
        this.porcentajeB = porcentajeB;
    }
}
