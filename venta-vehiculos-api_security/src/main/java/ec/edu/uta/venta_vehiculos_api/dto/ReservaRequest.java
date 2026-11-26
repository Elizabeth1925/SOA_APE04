/**
 * Archivo: ReservaRequest.java
 * Responsabilidad arquitectónica:
 *   Objeto de Transferencia de Datos (DTO) inmutable implementado como Java record
 *   que define el contrato de entrada de POST /api/vehiculos/{id}/reservas.
 *   Valida el formato del identificador único de operación (operacionId) que garantiza
 *   idempotencia en la creación de reservas.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Contrato de entrada de la fase 1 de orquestación en 2 fases
 *     invocada por ventas-api (FastAPI) al reservar un vehículo antes de confirmar venta.
 *   - [Actividad 10 - P27]: Permite reintentos seguros e idempotentes ante balanceo
 *     entre Instancia A (:8080) e Instancia B (:8082) evitando reservas duplicadas.
 */

// Declara el paquete que agrupa los DTOs de la API.
package ec.edu.uta.venta_vehiculos_api.dto;

// Importa las anotaciones de Jakarta Bean Validation (@NotBlank, @Pattern).
import jakarta.validation.constraints.*;

// [Actividad 4] [Actividad 10 - P27] Record inmutable que encapsula el identificador de operación de la reserva.
public record ReservaRequest(
    // Exige que operacionId no sea nulo ni esté compuesto únicamente por espacios en blanco.
    @NotBlank 
    // Limita operacionId a caracteres alfanuméricos y guiones (1 a 64 caracteres) para seguridad e idempotencia.
    @Pattern(regexp="[A-Za-z0-9-]{1,64}")
    // Identificador de negocio único generado por ventas-api para correlacionar reintentos de la misma venta.
    String operacionId
) {}
