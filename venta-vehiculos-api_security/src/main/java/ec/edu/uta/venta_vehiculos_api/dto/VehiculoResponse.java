/**
 * Archivo: VehiculoResponse.java
 * Responsabilidad arquitectónica:
 *   DTO inmutable (Java record) que representa la proyección pública serializada en
 *   JSON de un vehículo para todas las respuestas de VehiculoController (GET, POST,
 *   PUT y PATCH). Evita exponer directamente la entidad JPA Vehiculo en la capa web.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Define el contrato REST de salida del catálogo de vehículos.
 *   - [Actividad 4]: Expone el atributo "estado" (DISPONIBLE, RESERVADO, VENDIDO)
 *     que refleja la máquina de estados de la orquestación de ventas en 2 fases.
 *   - [Actividad 10 - P27]: Permite verificar la coherencia transversal del inventario
 *     consultando indistintamente la Instancia A (:8080) o la Instancia B (:8082).
 */

// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa BigDecimal para exponer el precio con precisión decimal exacta.
import java.math.BigDecimal;

// Importa la entidad JPA Vehiculo usada como fuente para el método de mapeo.
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

// [Actividad 1] [Actividad 4] [Actividad 10 - P27] Record inmutable que define el contrato JSON de salida de un vehículo.
public record VehiculoResponse(

    // Identificador numérico único (clave primaria) asignado al vehículo en base de datos.
    Integer idVehiculo,

    // Marca comercial y modelo del vehículo.
    String marca, String modelo,

    // Matrícula/placa, código VIN de chasis (17 caracteres) y año de fabricación.
    String placa, String chasis, Integer anio,

    // Color, precio decimal y estado actual del ciclo de vida (DISPONIBLE, RESERVADO o VENDIDO).
    String color, BigDecimal precio, String estado

) {

    // [Actividad 1] Método estático de fábrica que convierte una entidad JPA Vehiculo en su DTO de respuesta.
    public static VehiculoResponse de(Vehiculo v) {

        // Instancia VehiculoResponse copiando cada propiedad desde la entidad persistida.
        return new VehiculoResponse(

            // Copia el identificador primario del vehículo.
            v.getIdVehiculo(),
            // Copia la marca comercial del vehículo.
            v.getMarca(),
            // Copia el modelo del vehículo.
            v.getModelo(),
            // Copia la placa normalizada del vehículo.
            v.getPlaca(),
            // Copia el número de chasis normalizado del vehículo.
            v.getChasis(),
            // Copia el año de fabricación del vehículo.
            v.getAnio(),
            // Copia el color registrado del vehículo.
            v.getColor(),
            // Copia el precio monetario exacto del vehículo.
            v.getPrecio(),
            // [Actividad 4] Copia el estado vigente (DISPONIBLE, RESERVADO o VENDIDO).
            v.getEstado()
        );
    }

}
