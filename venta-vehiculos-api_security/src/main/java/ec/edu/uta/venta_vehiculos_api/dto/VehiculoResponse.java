// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa BigDecimal para representar el precio de forma decimal precisa.
import java.math.BigDecimal;

// Importa la entidad desde la que se construirá la respuesta.
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

// Define los campos que se serializan en la respuesta de consulta de un vehículo.
public record VehiculoResponse(

    // Identificador único asignado al vehículo en la base de datos.
    Integer idVehiculo,

    // Marca comercial del vehículo.
    String marca, String modelo,

    // Placa, número de chasis y año de fabricación.
    String placa, String chasis, Integer anio,

    // Color, precio y estado actual del vehículo.
    String color, BigDecimal precio, String estado

) {

    // Convierte la entidad Vehiculo en el DTO usado para responder al cliente.
    public static VehiculoResponse de(Vehiculo v) {

        // Crea la respuesta copiando los valores públicos de la entidad.
        return new VehiculoResponse(

            // Copia el identificador de la entidad.
            v.getIdVehiculo(),
            // Copia la marca de la entidad.
            v.getMarca(),
            // Copia el modelo de la entidad.
            v.getModelo(),
            // Copia la placa de la entidad.
            v.getPlaca(),
            // Copia el número de chasis de la entidad.
            v.getChasis(),
            // Copia el año de fabricación de la entidad.
            v.getAnio(),
            // Copia el color de la entidad.
            v.getColor(),
            // Copia el precio de la entidad.
            v.getPrecio(),
            // Copia el estado de la entidad.
            v.getEstado()
        );
    }

}
