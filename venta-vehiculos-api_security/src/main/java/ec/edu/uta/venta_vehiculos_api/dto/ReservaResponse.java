/**
 * Archivo: ReservaResponse.java
 * Responsabilidad arquitectónica:
 *   DTO inmutable (Java record) que representa el estado serializado en JSON de una
 *   reserva devuelto por ReservaController hacia el orquestador ventas-api (FastAPI).
 *   Provee un método estático de fábrica para desacoplar la entidad JPA Reserva del
 *   contrato HTTP expuesto.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Contrato de respuesta en las fases de reserva, consulta y
 *     confirmación de la orquestación en 2 fases con ventas-api.
 *   - [Actividad 10 - P27]: Refleja de manera homogénea el precio congelado, moneda
 *     y estado de la reserva desde cualquiera de las dos instancias (A :8080 o B :8082).
 */

// Declara el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa BigDecimal para representar el monto reservado con exactitud decimal.
import java.math.BigDecimal;
// Importa la entidad JPA Reserva desde la cual se construye este DTO.
import ec.edu.uta.venta_vehiculos_api.model.Reserva;

// [Actividad 4] [Actividad 10 - P27] Record inmutable que expone los atributos públicos de una reserva.
public record ReservaResponse(
    // Identificador UUID único asignado a la reserva al crearse.
    String id, 
    // Identificador único de operación (clave de idempotencia enviada por ventas-api).
    String operacionId, 
    // Identificador primario del vehículo reservado.
    Integer vehiculoId,
    // Precio unitario del vehículo capturado al momento de ejecutar la reserva.
    BigDecimal precio, 
    // Código ISO de moneda asociado al precio reservado (ej. "USD").
    String moneda, 
    // Estado actual del ciclo de vida de la reserva (RESERVADA, CONFIRMADA o LIBERADA).
    String estado
) {

    // [Actividad 4] Método estático de fábrica que mapea una entidad JPA Reserva a su representación DTO.
    public static ReservaResponse de(Reserva r) {
        // Instancia el record copiando los campos persistidos sin exponer la entidad JPA gestionada.
        return new ReservaResponse(
            // Copia el UUID primario de la reserva.
            r.id,
            // Copia la clave de idempotencia de la operación.
            r.operacionId,
            // Extrae únicamente el identificador numérico del vehículo asociado.
            r.vehiculo.getIdVehiculo(),
            // Copia el precio congelado de la reserva.
            r.precio,
            // Copia el código de moneda de la reserva.
            r.moneda,
            // Copia el estado actual de la reserva (RESERVADA, CONFIRMADA o LIBERADA).
            r.estado
        );
    }
}
