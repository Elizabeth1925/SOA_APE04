package ec.edu.uta.venta_vehiculos_api.dto;

import jakarta.validation.constraints.*;

// DTO inmutable (record) para recibir la petición de reserva
public record ReservaRequest(
    // Identificador alfanumérico de la operación que no puede estar vacío ni contener caracteres especiales no permitidos
    @NotBlank 
    @Pattern(regexp="[A-Za-z0-9-]{1,64}")
    String operacionId
) {}
