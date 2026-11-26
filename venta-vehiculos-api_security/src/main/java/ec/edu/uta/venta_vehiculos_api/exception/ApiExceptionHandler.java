/**
 * Archivo: ApiExceptionHandler.java
 * Responsabilidad arquitectónica:
 *   Manejador global de excepciones REST (@RestControllerAdvice) que intercepta
 *   errores provenientes de cualquier controlador o servicio y los traduce a
 *   respuestas estandarizadas RFC 7807 (application/problem+json mediante ProblemDetail),
 *   incorporando URI de tipo, instancia solicitada, código de negocio y traceId de
 *   correlación. Complementa los rechazos HTTP 401/403 RFC 7807 generados directamente
 *   en la cadena de filtros de Spring Security (SecurityConfig e IntegrationFilter).
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Garantiza contratos HTTP de error consistentes para validaciones
 *     (422), JSON mal formado (400), conflictos de integridad (409) y no encontrados (404).
 *   - [Actividad 4]: Traduce conflictos de reserva/estado (ApiError 409/404) hacia
 *     ventas-api con formato estructurado Problem Details.
 *   - [Actividad 11]: Unifica el estándar RFC 7807 (application/problem+json) para toda
 *     la API junto con los rechazos 401/403 de seguridad, evitando fuga de stacktraces
 *     en respuestas cliente (registrándolos solo en servidor cuando status >= 500).
 */

// Identifica el paquete que contiene el manejo centralizado de errores de la API.
package ec.edu.uta.venta_vehiculos_api.exception;

// Importa URI para construir los atributos estándar "type" e "instance" del formato RFC 7807.
import java.net.URI;

// Importa UUID para generar un identificador único de trazabilidad (traceId) por cada incidente.
import java.util.UUID;

// Importa LoggerFactory de SLF4J para registrar errores internos (5xx) en los logs del servidor.
import org.slf4j.LoggerFactory;

// Importa DataIntegrityViolationException para traducir violaciones de unicidad o llaves foráneas a HTTP 409.
import org.springframework.dao.DataIntegrityViolationException;

// Importa HttpStatusCode, MediaType, ProblemDetail y ResponseEntity de Spring Web.
import org.springframework.http.*;

// Importa HttpMessageNotReadableException para detectar cuerpos JSON sintácticamente inválidos o mal tipados.
import org.springframework.http.converter.HttpMessageNotReadableException;

// Importa MethodArgumentNotValidException para capturar errores de Jakarta Bean Validation (@Valid).
import org.springframework.web.bind.MethodArgumentNotValidException;

// Importa @RestControllerAdvice y @ExceptionHandler para la interceptación global de excepciones.
import org.springframework.web.bind.annotation.*;

// Importa ErrorResponse para manejar excepciones nativas de Spring MVC que ya portan un estado HTTP.
import org.springframework.web.ErrorResponse;

// Importa HttpServletRequest para extraer la ruta solicitada (req.getRequestURI()) en el campo instance.
import jakarta.servlet.http.HttpServletRequest;

// [Actividad 1] [Actividad 11] Aplica este manejador transversalmente a todos los controladores REST.
@RestControllerAdvice
// Traduce cualquier excepción no capturada en los controladores a una respuesta RFC 7807 uniforme.
public class ApiExceptionHandler {

    // [Actividad 1] [Actividad 4] [Actividad 11] Intercepta cualquier Exception lanzada durante el procesamiento de un endpoint.
    @ExceptionHandler(Exception.class)

    // Clasifica la excepción, registra trazas si es error 5xx y construye la respuesta application/problem+json.
    public ResponseEntity<ProblemDetail> manejar(
            // Excepción capturada y solicitud HTTP que originó el error.
            Exception ex, HttpServletRequest req) {

        // Valor por defecto para fallos inesperados del servidor: HTTP 500 Internal Server Error.
        int status = 500;

        // Código simbólico por defecto para errores internos no clasificados.
        String codigo = "ERROR_INTERNO";

        // [Actividad 11] Mensaje genérico seguro que evita exponer detalles internos de infraestructura o SQL.
        String detalle = "No fue posible completar la solicitud";

        // [Actividad 1] [Actividad 4] Verifica si se trata de una excepción controlada de negocio (ApiError).
        if (ex instanceof ApiError e) {

            // Respeta el código HTTP definido por la regla de negocio (ej. 404, 409, 422).
            status = e.status;
            // Respeta el código simbólico de negocio (ej. CONFLICTO_RESERVA, VEHICULO_NO_DISPONIBLE).
            codigo = e.codigo;
            // Expone el mensaje controlado y apto para el cliente definido al lanzar ApiError.
            detalle = e.getMessage();

        // [Actividad 1] Verifica si fallaron las validaciones declarativas (@Valid) en los DTOs de entrada.
        } else if (ex instanceof MethodArgumentNotValidException e) {

            // Clasifica los errores de validación semántica con HTTP 422 Unprocessable Entity.
            status = 422;
            // Asigna el código estable VALIDACION para facilitar manejo programático por clientes.
            codigo = "VALIDACION";

            // Recorre todos los errores por campo del BindingResult para resumirlos en una sola cadena.
            detalle = e.getBindingResult().getFieldErrors().stream()

                // Formatea cada violación como "nombreCampo: mensajeDeError".
                .map(x -> x.getField() + ": " + x.getDefaultMessage())

                // Concatena múltiples errores de campo separados por "; ".
                .reduce((a,b) -> a + "; " + b)

                // Aplica un mensaje de respaldo si no hubo errores asociados a campos específicos.
                .orElse("Datos inválidos");

        // [Actividad 1] Verifica si el cuerpo HTTP no pudo deserializarse (JSON roto o tipo incompatible).
        } else if (ex instanceof HttpMessageNotReadableException) {

            // Devuelve HTTP 400 Bad Request por solicitud sintácticamente incorrecta.
            status = 400;
            // Asigna el código estable JSON_INVALIDO.
            codigo = "JSON_INVALIDO";
            // Informa el error de lectura sin exponer clases internas de Jackson.
            detalle = "JSON ilegible o tipo de dato incorrecto";

        // [Actividad 1] [Actividad 4] Verifica violaciones de restricciones relacionales (clave única placa/chasis/operacion_id o FK).
        } else if (ex instanceof DataIntegrityViolationException) {

            // Devuelve HTTP 409 Conflict por colisión con datos persistidos en MariaDB.
            status = 409;
            // Asigna el código estable CONFLICTO_DATOS.
            codigo = "CONFLICTO_DATOS";
            // Describe el conflicto en forma abstracta sin filtrar nombres de restricciones o sentencias SQL.
            detalle = "Duplicado o relación que impide la operación";

        // [Actividad 1] Verifica argumentos inválidos detectados por la lógica de aplicación.
        } else if (ex instanceof IllegalArgumentException) {

            // Traduce el argumento inválido a HTTP 400 Bad Request.
            status = 400;
            // Asigna el código estable SOLICITUD_INVALIDA.
            codigo = "SOLICITUD_INVALIDA";
            // Transfiere el mensaje explicativo del argumento rechazado.
            detalle = ex.getMessage();

        // [Actividad 1] [Actividad 11] Reconoce excepciones estándar de Spring Web (ej. 405 Method Not Allowed, 415 Unsupported Media Type).
        } else if (ex instanceof ErrorResponse e) {

            // Extrae el código HTTP provisto por la excepción estándar de Spring.
            status = e.getStatusCode().value();

            // Asigna el código genérico ERROR_HTTP para errores de protocolo MVC.
            codigo = "ERROR_HTTP";

            // Recupera el detalle incluido en el ProblemDetail interno de Spring.
            detalle = e.getBody().getDetail();
        }

        // Genera un UUID de correlación que vincula la respuesta entregada al cliente con el log interno.
        String trace = UUID.randomUUID().toString();

        // Registra el stacktrace completo exclusivamente para fallos del servidor (HTTP 5xx).
        if (status >= 500)
            // Emite el registro de error con el traceId y la excepción completa mediante SLF4J.
            LoggerFactory.getLogger(getClass())
                .error("Error " + trace, ex);

        // [Actividad 11] Crea el objeto RFC 7807 ProblemDetail con el estado HTTP y el detalle determinados.
        ProblemDetail p = ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(status), detalle);

        // Asigna el URI de tipo estándar RFC 7807 bajo el espacio de nombres urn:vehiculos:<codigo>.
        p.setType(URI.create("urn:vehiculos:" + codigo.toLowerCase()));

        // Asigna el URI de instancia RFC 7807 apuntando a la ruta exacta de la solicitud fallida.
        p.setInstance(URI.create(req.getRequestURI()));

        // Añade la propiedad de extensión "codigo" para diagnóstico automatizado.
        p.setProperty("codigo", codigo);

        // Añade la propiedad de extensión "traceId" para rastreo en bitácoras del servidor.
        p.setProperty("traceId", trace);

        // [Actividad 11] Devuelve la respuesta HTTP con Content-Type: application/problem+json y el cuerpo RFC 7807.
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(p);

    }

}
