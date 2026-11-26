/**
 * Archivo: ReservaController.java
 * Responsabilidad arquitectónica:
 *   Controlador REST interno dedicado al ciclo de vida de reservas de vehículos
 *   (crear reserva, consultar por ID, consultar por operacionId, confirmar venta
 *   y liberar reserva). Sus rutas solo son invocables por el microservicio
 *   orquestador ventas-api autenticado mediante clave interna.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Implementa los endpoints de la orquestación en 2 fases con
 *     ventas-api (FastAPI): fase 1 de reserva idempotente (DISPONIBLE -> RESERVADO)
 *     y fase 2 de confirmación (RESERVADO -> VENDIDO) o compensación/liberación
 *     (RESERVADO -> DISPONIBLE).
 *   - [Actividad 10 - P27]: Garantiza operaciones consistentes sobre el inventario
 *     y reservas sin importar si NGINX enruta la petición a la Instancia A (:8080)
 *     o a la Instancia B (:8082).
 *   - [Actividad 11]: Todos sus endpoints quedan restringidos al rol interno
 *     ROLE_SERVICIO_VENTAS asignado por IntegrationFilter mediante X-Service-Key.
 */

// Declara el paquete de controladores REST del microservicio.
package ec.edu.uta.venta_vehiculos_api.controller;

// Importa @Valid para activar la validación Bean Validation sobre el cuerpo ReservaRequest.
import jakarta.validation.Valid;
// Importa ResponseEntity para construir respuestas HTTP explícitas como 204 No Content.
import org.springframework.http.ResponseEntity;
// Importa las anotaciones REST de Spring MVC para mapear rutas, verbos y parámetros.
import org.springframework.web.bind.annotation.*;
// Importa los DTOs de solicitud y respuesta de reservas.
import ec.edu.uta.venta_vehiculos_api.dto.*;
// Importa el servicio transaccional que gestiona reservas y bloqueos pesimistas.
import ec.edu.uta.venta_vehiculos_api.service.ReservaService;

// [Actividad 4] [Actividad 11] Expone esta clase como controlador REST de endpoints internos de reserva.
@RestController
// Controlador que publica la API interna de reservas consumida por ventas-api (FastAPI).
public class ReservaController {

    // Dependencia inmutable hacia la capa de servicio transaccional de reservas.
    private final ReservaService service;

    // Constructor con inyección de dependencias de ReservaService provista por Spring.
    public ReservaController(ReservaService service) {
        // Guarda la referencia del servicio para delegar cada operación HTTP.
        this.service = service;
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 1 de orquestación: reserva un vehículo de forma idempotente mediante operacionId.
    @PostMapping("/api/vehiculos/{id}/reservas")
    // Recibe el ID del vehículo por ruta y valida el cuerpo JSON ReservaRequest antes de invocar al servicio.
    public ReservaResponse reservar(
            // Extrae el identificador numérico del vehículo desde el segmento {id} de la URL.
            @PathVariable int id,
            // Deserializa y valida el DTO con el identificador único de operación (operacionId).
            @Valid @RequestBody ReservaRequest d) {
        // Delega la reserva con bloqueo pesimista nativo FOR UPDATE y retorna el estado resultante.
        return service.reservar(id, d.operacionId());
    }

    // [Actividad 4] [Actividad 10 - P27] Consulta una reserva existente mediante su identificador UUID.
    @GetMapping("/api/reservas/{id}")
    // Extrae el UUID de la reserva desde la ruta y consulta su estado actualizado en base de datos.
    public ReservaResponse obtener(@PathVariable String id) {
        // Delega la búsqueda en modo solo lectura al servicio de reservas.
        return service.obtener(id);
    }

    // [Actividad 4] [Actividad 10 - P27] Consulta una reserva por su clave de idempotencia ?operacionId=... para recuperación ante reintentos.
    @GetMapping("/api/reservas")
    // Extrae el parámetro de consulta operacionId enviado por ventas-api para verificar estado previo.
    public ReservaResponse operacion(@RequestParam String operacionId) {
        // Delega la búsqueda por operacionId al servicio y devuelve el DTO de la reserva.
        return service.porOperacion(operacionId);
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 2 (commit): confirma la reserva cambiando vehículo de RESERVADO a VENDIDO.
    @PutMapping("/api/reservas/{id}/confirmacion")
    // Recibe el UUID de la reserva a confirmar y ejecuta la transición definitiva a venta.
    public ReservaResponse confirmar(@PathVariable String id) {
        // Delega la confirmación transaccional con bloqueo pesimista al servicio.
        return service.confirmar(id);
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 2 (compensación/rollback): libera la reserva devolviendo el vehículo a DISPONIBLE.
    @DeleteMapping("/api/reservas/{id}")
    // Recibe el UUID de la reserva a cancelar y devuelve HTTP 204 No Content si se libera correctamente.
    public ResponseEntity<Void> liberar(@PathVariable String id) {
        // Delega la liberación transaccional con bloqueo pesimista al servicio.
        service.liberar(id);
        // Retorna HTTP 204 No Content sin cuerpo confirmando la liberación exitosa.
        return ResponseEntity.noContent().build();
    }
}
