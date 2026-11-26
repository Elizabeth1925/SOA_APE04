/**
 * Archivo: VehiculoRequest.java
 * Responsabilidad arquitectónica:
 *   DTO inmutable (Java record) que define y valida el contrato JSON de entrada
 *   para las operaciones POST /api/vehiculos y PUT /api/vehiculos/{id}. Aplica
 *   reglas Jakarta Bean Validation que son convertidas a HTTP 422 Problem Details
 *   por ApiExceptionHandler en caso de incumplimiento.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Define el contrato REST de entrada y validaciones estructurales
 *     del recurso vehículos.
 *   - [Actividad 10 - P27]: Garantiza que solo ingresen datos consistentes y
 *     normalizables al inventario compartido entre Instancia A (:8080) e Instancia B (:8082).
 */

// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa BigDecimal para representar importes monetarios sin errores de punto flotante binario.
import java.math.BigDecimal;

// Importa las restricciones declarativas de Jakarta Bean Validation (@NotBlank, @Size, @NotNull, @Positive, @Digits).
import jakarta.validation.constraints.*;

// [Actividad 1] [Actividad 10 - P27] Record inmutable que modela los campos obligatorios y opcionales de un vehículo.
public record VehiculoRequest(

    // Valida que la marca no sea nula ni cadena vacía o solo espacios.
    @NotBlank String marca,

    // Valida que el modelo no sea nulo ni cadena vacía o solo espacios.
    @NotBlank String modelo,

    // Valida que la placa no sea nula ni cadena vacía o solo espacios.
    @NotBlank String placa,

    // Valida que el número de chasis (VIN) no esté en blanco y posea exactamente 17 caracteres.
    @NotBlank @Size(min=17, max=17) String chasis,

    // Valida que el año de fabricación esté presente (su rango numérico se verifica además en VehiculoService).
    @NotNull Integer anio,

    // Campo opcional que almacena el color del vehículo.
    String color,

    // Valida que el precio sea obligatorio, estrictamente mayor a cero y con hasta 10 dígitos enteros y 2 decimales.
    @NotNull @Positive @Digits(integer=10, fraction=2)
    BigDecimal precio

// Cierra la definición del DTO record VehiculoRequest.
) {}
