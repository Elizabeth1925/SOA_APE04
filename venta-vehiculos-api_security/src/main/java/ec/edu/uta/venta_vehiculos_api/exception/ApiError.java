/**
 * Archivo: ApiError.java
 * Responsabilidad arquitectónica:
 *   Excepción de dominio no verificada (RuntimeException) utilizada por las capas de
 *   servicio (VehiculoService, ReservaService) para señalizar errores controlados de
 *   negocio junto con su código HTTP exacto (404, 409, 422), código estable de
 *   aplicación y detalle legible para el cliente. Es interceptada por
 *   ApiExceptionHandler para emitir respuestas RFC 7807 (Problem Details).
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Estandariza los errores semánticos de los contratos REST de vehículos.
 *   - [Actividad 4]: Modela conflictos transaccionales y de estado (HTTP 409) durante
 *     las fases de reserva, confirmación y liberación en la orquestación con ventas-api.
 *   - [Actividad 11]: Contribuye a respuestas de error homogéneas en formato RFC 7807
 *     sin filtrar trazas internas ni información sensible del servidor.
 */

// Identifica el paquete que agrupa las excepciones y el manejador global de errores.
package ec.edu.uta.venta_vehiculos_api.exception;

// [Actividad 1] [Actividad 4] [Actividad 11] Excepción controlada que transporta estado HTTP y código de negocio hacia ApiExceptionHandler.
public class ApiError extends RuntimeException {
 // Código de estado HTTP asociado al error de negocio (ej. 404, 409, 422).
 public final int status;
 // Código simbólico estable de aplicación (ej. VEHICULO_NO_ENCONTRADO, CONFLICTO_RESERVA).
 public final String codigo;
 // Constructor que inicializa el estado HTTP, código simbólico y mensaje descriptivo del error.
 public ApiError(int status, String codigo, String detalle) {
  // Delega el mensaje legible al constructor de RuntimeException (recuperable vía getMessage()).
  super(detalle);
  // Almacena de forma inmutable el código de estado HTTP.
  this.status = status;
  // Almacena de forma inmutable el identificador simbólico del error.
  this.codigo = codigo;
 }
}
