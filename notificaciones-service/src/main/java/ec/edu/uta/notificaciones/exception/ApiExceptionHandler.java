/**
 * ==============================================================================
 * ARCHIVO: ApiExceptionHandler.java
 * COMPONENTE: notificaciones-service (Manejador Global de Excepciones REST)
 * ROL: Intercepta errores de validacion de negocio (IllegalArgumentException)
 *      lanzados desde la capa de servicio y los traduce a respuestas HTTP 400
 *      estructuradas en JSON sin exponer trazas internas del servidor.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Control de errores consistente en la API
 *     REST de notificaciones.
 * ==============================================================================
 */
// Declara el paquete de manejo centralizado de excepciones del servicio.
package ec.edu.uta.notificaciones.exception;

// Importa el DTO uniforme de respuesta utilizado para devolver el detalle del error.
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
// Importa la enumeracion de codigos HTTP (BAD_REQUEST = 400).
import org.springframework.http.HttpStatus;
// Importa ResponseEntity para construir la respuesta HTTP con estado y cuerpo JSON.
import org.springframework.http.ResponseEntity;
// Importa @ExceptionHandler para asociar el metodo al tipo de excepcion indicado.
import org.springframework.web.bind.annotation.ExceptionHandler;
// Importa @RestControllerAdvice para aplicar este manejador globalmente a todos los controladores REST.
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Componente asesor global que transforma excepciones de validacion en respuestas
 * HTTP 400 Bad Request legibles para el cliente.
 */
// Registra el manejador global de excepciones para todos los @RestController del microservicio.
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Captura excepciones de argumentos invalidos originadas en validaciones de entrada
     * (por ejemplo campos nulos, vacios o tipo de notificacion no permitido).
     *
     * @param ex excepcion IllegalArgumentException lanzada por NotificacionService.
     * @return respuesta HTTP 400 Bad Request con NotificacionResponse(false, null, mensaje).
     */
    // Asocia este metodo al manejo de cualquier IllegalArgumentException no capturada en los controladores.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<NotificacionResponse> manejarValidacion(
            // Recibe la instancia de la excepcion para extraer su mensaje de validacion.
            IllegalArgumentException ex) {

        // Construye el DTO de error indicando fallo (success=false), estado nulo y el mensaje de negocio.
        NotificacionResponse response =
                new NotificacionResponse(
                        false,
                        null,
                        ex.getMessage()
                );

        // Retorna codigo HTTP 400 (Bad Request) junto con el cuerpo JSON estandarizado.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }
}
