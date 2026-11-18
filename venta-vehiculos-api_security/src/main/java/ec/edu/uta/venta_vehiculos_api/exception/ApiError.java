// Identifica el paquete que contiene los tipos de error propios de la API.
package ec.edu.uta.venta_vehiculos_api.exception;

// Excepción de ejecución que transporta el estado HTTP y el código de error de la API.
public class ApiError extends RuntimeException {
 // Conserva el estado HTTP que se devolverá al cliente.
 public final int status;
 // Conserva el código legible y estable que identifica el tipo de error.
 public final String codigo;
 // Crea el error con estado, código y detalle descriptivo.
 public ApiError(int status, String codigo, String detalle) {
  // Guarda el detalle como mensaje estándar de la excepción.
  super(detalle);
  // Almacena el estado para que el manejador global forme la respuesta HTTP.
  this.status = status;
  // Almacena el código para incluirlo en el cuerpo de error.
  this.codigo = codigo;
 }
}