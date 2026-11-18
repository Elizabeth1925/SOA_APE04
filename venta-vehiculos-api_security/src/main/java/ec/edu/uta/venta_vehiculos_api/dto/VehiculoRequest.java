// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa BigDecimal para representar el precio sin errores de precisión binaria.
import java.math.BigDecimal;

// Importa las restricciones usadas para validar los datos recibidos.
import jakarta.validation.constraints.*;

// Define inmutablemente los datos que se aceptan al crear o actualizar un vehículo.
public record VehiculoRequest(

    // Exige que la marca esté presente y no contenga solamente espacios.
    @NotBlank String marca,

    // Exige que el modelo esté presente y no contenga solamente espacios.
    @NotBlank String modelo,

    // Exige que la placa esté presente y no contenga solamente espacios.
    @NotBlank String placa,

    // Exige un chasis no vacío con exactamente 17 caracteres.
    @NotBlank @Size(min=17, max=17) String chasis,

    // Exige que se indique el año de fabricación.
    @NotNull Integer anio,

    // Acepta el color como dato opcional.
    String color,

    // Exige un precio positivo con hasta 10 dígitos enteros y 2 decimales.
    @NotNull @Positive @Digits(integer=10, fraction=2)
    BigDecimal precio

// Cierra la declaración del DTO con sus campos validados.
) {}
