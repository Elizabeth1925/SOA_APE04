package ec.edu.uta.venta_vehiculos_api.dto;

import java.math.BigDecimal;
import ec.edu.uta.venta_vehiculos_api.model.Reserva;

// DTO inmutable (record) para retornar los detalles de una reserva creada o consultada
public record ReservaResponse(
    String id, 
    String operacionId, 
    Integer vehiculoId,
    BigDecimal precio, 
    String moneda, 
    String estado
) {

    // Método estático de fábrica para mapear desde la entidad Reserva hacia el DTO de respuesta
    public static ReservaResponse de(Reserva r) {
        return new ReservaResponse(
            r.id,
            r.operacionId,
            r.vehiculo.getIdVehiculo(),
            r.precio,
            r.moneda,
            r.estado
        );
    }
}
