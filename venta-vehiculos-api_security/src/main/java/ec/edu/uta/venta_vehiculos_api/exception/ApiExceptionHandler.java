// Identifica el paquete que contiene el manejo de errores de la API.
package ec.edu.uta.venta_vehiculos_api.exception;

// Importa URI para construir los campos type e instance de Problem Details.
import java.net.URI;

// Importa UUID para generar un identificador de seguimiento para cada error.
import java.util.UUID;

// Importa el logger usado para registrar errores internos del servidor.
import org.slf4j.LoggerFactory;

// Permite reconocer conflictos de integridad reportados por la base de datos.
import org.springframework.dao.DataIntegrityViolationException;

// Importa los tipos HTTP usados para crear respuestas y detalles de problema.
import org.springframework.http.*;

// Permite reconocer cuerpos HTTP ilegibles o JSON mal formado.
import org.springframework.http.converter.HttpMessageNotReadableException;

// Permite reconocer errores de validación de argumentos de métodos.
import org.springframework.web.bind.MethodArgumentNotValidException;

// Importa las anotaciones MVC para el consejo y la captura de excepciones.
import org.springframework.web.bind.annotation.*;

// Proporciona los datos de respuestas HTTP estándar de Spring.
import org.springframework.web.ErrorResponse;

// Permite consultar información de la petición que produjo el error.
import jakarta.servlet.http.HttpServletRequest;

// Aplica el manejador a excepciones lanzadas por cualquier controlador REST.
@RestControllerAdvice
// Convierte excepciones de la aplicación en respuestas HTTP de error uniformes.
public class ApiExceptionHandler {

    // Registra el método siguiente como manejador general de excepciones.
    @ExceptionHandler(Exception.class)

    // Construye una respuesta Problem Details con el estado y detalle adecuados.
    public ResponseEntity<ProblemDetail> manejar(
            // Excepción original que se traducirá a una respuesta de API.
            Exception ex, HttpServletRequest req) {

        // Usa 500 como estado inicial para errores no clasificados.
        int status = 500;

        // Define el código inicial para errores internos no clasificados.
        String codigo = "ERROR_INTERNO";

        // Define un mensaje genérico para no revelar detalles internos por defecto.
        String detalle = "No fue posible completar la solicitud";

        // Comprueba si la excepción contiene un estado y código definidos por la API.
        if (ex instanceof ApiError e) {

            // Conserva el estado HTTP definido en la excepción propia.
            status = e.status;
            // Conserva el código de error definido en la excepción propia.
            codigo = e.codigo;
            // Usa el mensaje asociado a la excepción como detalle de respuesta.
            detalle = e.getMessage();

        // Detecta errores generados por las restricciones de validación de entrada.
        } else if (ex instanceof MethodArgumentNotValidException e) {

            // Representa los datos rechazados como error de entidad no procesable.
            status = 422;
            // Identifica la respuesta como un error de validación.
            codigo = "VALIDACION";

            // Obtiene los errores por campo y comienza a construir un texto legible.
            detalle = e.getBindingResult().getFieldErrors().stream()

                // Convierte cada error en el formato "campo: descripción".
                .map(x -> x.getField() + ": " + x.getDefaultMessage())

                // Une los mensajes de campos diferentes en una sola cadena.
                .reduce((a,b) -> a + "; " + b)

                // Usa un mensaje general si la validación no produjo errores de campo.
                .orElse("Datos inválidos");

        // Detecta cuerpos de solicitud ilegibles o JSON incompatible con el DTO.
        } else if (ex instanceof HttpMessageNotReadableException) {

            // Indica que el cuerpo recibido no pudo interpretarse.
            status = 400;
            // Asigna un código estable para errores de formato JSON.
            codigo = "JSON_INVALIDO";
            // Describe al cliente el problema de lectura de la solicitud.
            detalle = "JSON ilegible o tipo de dato incorrecto";

        // Detecta violaciones de restricciones o conflictos de integridad de datos.
        } else if (ex instanceof DataIntegrityViolationException) {

            // Indica que la solicitud entra en conflicto con datos persistidos.
            status = 409;
            // Asigna el código que identifica el conflicto de datos.
            codigo = "CONFLICTO_DATOS";
            // Describe el tipo general de conflicto sin exponer datos de base.
            detalle = "Duplicado o relación que impide la operación";

        // Detecta argumentos inválidos enviados a la lógica de aplicación.
        } else if (ex instanceof IllegalArgumentException) {

            // Traduce el argumento inválido a una solicitud HTTP incorrecta.
            status = 400;
            // Asigna el código de solicitud inválida.
            codigo = "SOLICITUD_INVALIDA";
            // Devuelve el mensaje de validación de la lógica del servicio.
            detalle = ex.getMessage();

        // Reconoce otras excepciones que ya transportan un estado HTTP.
        } else if (ex instanceof ErrorResponse e) {

            // Toma el código de estado definido por la respuesta original.
            status = e.getStatusCode().value();

            // Identifica este caso como un error HTTP general.
            codigo = "ERROR_HTTP";

            // Obtiene el detalle de la respuesta estándar de Spring.
            detalle = e.getBody().getDetail();
        }

        // Genera un identificador único para correlacionar esta respuesta con los logs.
        String trace = UUID.randomUUID().toString();

        // Registra la traza completa solo para errores internos del servidor.
        if (status >= 500)
            // Adjunta la excepción y el identificador al registro de errores.
            LoggerFactory.getLogger(getClass())
                .error("Error " + trace, ex);

        // Construye el objeto Problem Details con estado y mensaje finales.
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), detalle);

        // Define un URI que identifica la categoría del problema.
        p.setType(URI.create("urn:vehiculos:" + codigo.toLowerCase()));

        // Registra la ruta solicitada como instancia del problema.
        p.setInstance(URI.create(req.getRequestURI()));

        // Agrega el código de aplicación al cuerpo JSON de error.
        p.setProperty("codigo", codigo);

        // Agrega el identificador que permite rastrear el evento en los logs.
        p.setProperty("traceId", trace);

        // Devuelve el estado calculado con el tipo de contenido Problem Details y el cuerpo construido.
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(p);

    }

}
