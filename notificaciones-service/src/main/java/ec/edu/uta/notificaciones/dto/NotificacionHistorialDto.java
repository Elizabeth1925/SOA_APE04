package ec.edu.uta.notificaciones.dto;

import java.time.LocalDateTime;

public class NotificacionHistorialDto {
    private Long id;
    private String eventoId;
    private String tipo;
    private Integer ventaId;
    private Integer vehiculoId;
    private Integer compradorId;
    private String operacionId;
    private String mensaje;
    private String estado;
    private String fechaRegistro;

    public NotificacionHistorialDto() {
    }

    public NotificacionHistorialDto(Long id, String eventoId, String tipo, Integer ventaId, Integer vehiculoId, Integer compradorId, String operacionId, String mensaje, String estado, String fechaRegistro) {
        this.id = id;
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.ventaId = ventaId;
        this.vehiculoId = vehiculoId;
        this.compradorId = compradorId;
        this.operacionId = operacionId;
        this.mensaje = mensaje;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(String fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
