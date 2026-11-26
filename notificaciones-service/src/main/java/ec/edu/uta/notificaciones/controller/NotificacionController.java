/**
 * ==============================================================================
 * ARCHIVO: NotificacionController.java
 * COMPONENTE: notificaciones-service (Controlador REST)
 * ROL: Expone la interfaz HTTP /api/notificaciones para consultar el historial
 *      de notificaciones procesadas (GET) y registrar avisos directos (POST).
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Exposicion de GET /api/notificaciones sin
 *     clave interna (habilitado por NotificacionKeyFilter) para el consumo del
 *     cliente JavaFX a traves de NGINX (:8090), y POST protegido por clave.
 *   - [Actividad 10 - P27]: Entrega al cliente JavaFX del historial con
 *     ventaId y operacionId provenientes de los eventos de venta.
 * ==============================================================================
 */
// Declara el paquete de controladores REST del microservicio de notificaciones.
package ec.edu.uta.notificaciones.controller;

// Importa el DTO que representa cada registro del historial devuelto en GET /api/notificaciones.
import ec.edu.uta.notificaciones.dto.NotificacionHistorialDto;
// Importa el DTO de entrada para notificaciones enviadas manualmente por HTTP POST.
import ec.edu.uta.notificaciones.dto.NotificacionRequest;
// Importa el DTO de respuesta estandarizada para operaciones POST.
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
// Importa el servicio de negocio que gestiona el historial y el procesamiento de notificaciones.
import ec.edu.uta.notificaciones.service.NotificacionService;
// Importa la enumeracion de codigos de estado HTTP (usada para retornar 201 CREATED).
import org.springframework.http.HttpStatus;
// Importa ResponseEntity para construir respuestas HTTP con codigo de estado y cuerpo JSON explicito.
import org.springframework.http.ResponseEntity;
// Importa las anotaciones web de Spring MVC (@RestController, @RequestMapping, @GetMapping, @PostMapping, @RequestBody).
import org.springframework.web.bind.annotation.*;

// Importa la interfaz List para devolver la coleccion ordenada del historial de notificaciones.
import java.util.List;

/**
 * Controlador REST que atiende las solicitudes sobre el recurso /api/notificaciones.
 */
// Marca la clase como controlador REST (serializa automaticamente los retornos a JSON).
@RestController
// [Actividad 8 y Actividad 9] Define la ruta base enrutada por NGINX desde http://127.0.0.1:8090/api/notificaciones.
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    // Referencia inmutable al servicio que mantiene el historial concurrente y valida las notificaciones.
    private final NotificacionService notificacionService;

    /**
     * Constructor con inyeccion de dependencias del servicio de notificaciones.
     *
     * @param notificacionService servicio de negocio inyectado por Spring.
     */
    public NotificacionController(NotificacionService notificacionService) {
        // Guarda la referencia al servicio de notificaciones.
        this.notificacionService = notificacionService;
    }

    /**
     * Endpoint GET para consultar el historial completo de notificaciones (Requerido para JavaFX).
     * [Actividad 8 y Actividad 9]: Accesible sin cabecera X-Notificacion-Key gracias a NotificacionKeyFilter.
     * [Actividad 10 - P27]: Devuelve los registros mas recientes primero con ventaId y operacionId.
     *
     * @return lista inmutable de notificaciones ordenadas de la mas reciente a la mas antigua.
     */
    // Mapea peticiones HTTP GET sobre /api/notificaciones.
    @GetMapping
    public List<NotificacionHistorialDto> listar() {
        // Delega al servicio la obtencion del historial completo en orden cronologico inverso.
        return notificacionService.obtenerHistorial();
    }

    /**
     * Endpoint POST para registrar avisos directos.
     * [Actividad 8 y Actividad 9]: Protegido en NotificacionKeyFilter mediante X-Notificacion-Key.
     *
     * @param request cuerpo JSON con los datos de la notificacion manual a registrar.
     * @return ResponseEntity con estado HTTP 201 Created y el resultado del procesamiento.
     */
    // Mapea peticiones HTTP POST sobre /api/notificaciones.
    @PostMapping
    public ResponseEntity<NotificacionResponse> crear(
            // Deserializa el cuerpo JSON de la peticion al objeto NotificacionRequest.
            @RequestBody NotificacionRequest request) {

        // Valida y registra la notificacion manual delegando la logica de negocio al servicio.
        NotificacionResponse response =
                notificacionService.procesar(request);

        // Construye y retorna una respuesta HTTP 201 CREATED con el DTO de confirmacion en el cuerpo.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
