package ec.edu.uta.monitor_balancer_service.dto;

public class EstadisticasResponse {

    private String inicioVentana;
    private String finVentana;
    private long totalPeticiones;
    private long total2xx;
    private long total4xx;
    private long total5xx;
    private double promedioRespuestaMs;
    private String criterioVentana;

    public EstadisticasResponse() {
    }

    public EstadisticasResponse(String inicioVentana, String finVentana, long totalPeticiones,
                                long total2xx, long total4xx, long total5xx,
                                double promedioRespuestaMs, String criterioVentana) {
        this.inicioVentana = inicioVentana;
        this.finVentana = finVentana;
        this.totalPeticiones = totalPeticiones;
        this.total2xx = total2xx;
        this.total4xx = total4xx;
        this.total5xx = total5xx;
        this.promedioRespuestaMs = promedioRespuestaMs;
        this.criterioVentana = criterioVentana;
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

    public long getTotalPeticiones() {
        return totalPeticiones;
    }

    public void setTotalPeticiones(long totalPeticiones) {
        this.totalPeticiones = totalPeticiones;
    }

    public long getTotal2xx() {
        return total2xx;
    }

    public void setTotal2xx(long total2xx) {
        this.total2xx = total2xx;
    }

    public long getTotal4xx() {
        return total4xx;
    }

    public void setTotal4xx(long total4xx) {
        this.total4xx = total4xx;
    }

    public long getTotal5xx() {
        return total5xx;
    }

    public void setTotal5xx(long total5xx) {
        this.total5xx = total5xx;
    }

    public double getPromedioRespuestaMs() {
        return promedioRespuestaMs;
    }

    public void setPromedioRespuestaMs(double promedioRespuestaMs) {
        this.promedioRespuestaMs = promedioRespuestaMs;
    }

    public String getCriterioVentana() {
        return criterioVentana;
    }

    public void setCriterioVentana(String criterioVentana) {
        this.criterioVentana = criterioVentana;
    }
}
