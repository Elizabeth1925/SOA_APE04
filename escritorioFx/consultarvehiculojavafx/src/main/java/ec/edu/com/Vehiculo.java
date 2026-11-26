/*
 * ============================================================================
 * Archivo: Vehiculo.java
 * Proyecto: Cliente de Escritorio JavaFX (consultarvehiculojavafx) - SOA APE 04
 * Función:
 *   Define el contrato DTO inmutable (Java Record) que modela cada vehículo
 *   devuelto por el servicio de catálogo (GET /api/vehiculos) a través del
 *   Gateway NGINX (:8090).
 * Actividades APE 04 cubiertas:
 *   - [Actividad 1 / Actividad 9]: Contrato de transferencia de datos (DTO)
 *     alineado con el JSON del microservicio de vehículos y resiliente ante
 *     campos adicionales mediante @JsonIgnoreProperties(ignoreUnknown = true).
 *   - [Actividad 9]: Modelo de filas de la tabla "Catálogo de Vehículos" en
 *     JavaFX.
 *   - [Actividad 10 - P27]: Refleja el campo "estado" actualizado en tiempo real
 *     cuando un vehículo pasa de DISPONIBLE a VENDIDO tras una venta en la PWA.
 * ============================================================================
 */
// Declara el paquete raíz del proyecto cliente JavaFX.
package ec.edu.com;

// Importa @JsonIgnoreProperties de Jackson para ignorar propiedades JSON no mapeadas [Actividad 1 / Actividad 9].
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
// Importa BigDecimal para representar el precio del vehículo con precisión decimal exacta.
import java.math.BigDecimal;

// Configura Jackson para descartar cualquier campo extra del JSON sin lanzar errores de deserialización [Actividad 1 / Actividad 9].
@JsonIgnoreProperties (ignoreUnknown = true)

// Declara el record inmutable Vehiculo con los atributos del contrato REST de vehículos [Actividad 1 / Actividad 9].
public record Vehiculo(
    // Identificador único del vehículo en el catálogo.
    int idVehiculo, 
    // Marca comercial del fabricante del vehículo.
    String marca, 
    // Nombre o referencia del modelo del vehículo.
    String modelo,
    // Número de placa vehicular asignada.
    String placa,
    // Código alfanumérico único de chasis (VIN) del vehículo.
    String chasis,
    // Año de fabricación del vehículo según el contrato JSON.
    int año, 
    // Color exterior registrado del vehículo.
    String color,
    // Precio unitario del vehículo con precisión monetaria exacta.
    BigDecimal precio,
    // Estado comercial del vehículo (ej. DISPONIBLE o VENDIDO tras operación en PWA) [Actividad 10 - P27].
    String estado) {
    
}
