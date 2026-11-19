package ec.edu.uta.notificaciones.dto;

import java.io.Serializable;

public class NotificacionVentaEvento implements Serializable {
    private String eventoId;
    private String tipo;
    private Integer ventaId;
    private Integer vehiculoId;
    private Integer compradorId;
    private String operacionId;
    private String fechaUTC;

    public NotificacionVentaEvento() {
    }

    public NotificacionVentaEvento(String eventoId, String tipo, Integer ventaId, Integer vehiculoId, Integer compradorId, String operacionId, String fechaUTC) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.ventaId = ventaId;
        this.vehiculoId = vehiculoId;
        this.compradorId = compradorId;
        this.operacionId = operacionId;
        this.fechaUTC = fechaUTC;
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public Integer getVentaId() {
        return ventaId;
    }

    public void setVentaId(Integer ventaId) {
        this.ventaId = ventaId;
    }

    public Integer getVehiculoId() {
        return vehiculoId;
    }

    public void setVehiculoId(Integer vehiculoId) {
        this.vehiculoId = vehiculoId;
    }

    public Integer getCompradorId() {
        return compradorId;
    }

    public void setCompradorId(Integer compradorId) {
        this.compradorId = compradorId;
    }

    public String getOperacionId() {
        return operacionId;
    }

    public void setOperacionId(String operacionId) {
        this.operacionId = operacionId;
    }

    public String getFechaUTC() {
        return fechaUTC;
    }

    public void setFechaUTC(String fechaUTC) {
        this.fechaUTC = fechaUTC;
    }
}
