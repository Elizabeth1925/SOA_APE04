package ec.edu.com;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Record que representa una notificación de venta recibida a través de RabbitMQ.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificacionVentaDto(
    Long id,
    String eventoId,
    String tipo,
    Long ventaId,
    Long vehiculoId,
    Long compradorId,
    String mensaje,
    String estado,
    String fecha
) {}
