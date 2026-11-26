/**
 * Archivo: VehiculoController.java
 * Responsabilidad arquitectónica:
 *   Controlador REST público (protegido por JWT/RBAC) que expone el contrato CRUD
 *   completo del recurso /api/vehiculos: listado con filtro opcional por marca,
 *   consulta individual, alta, actualización total (PUT), modificación parcial de
 *   precio (PATCH application/merge-patch+json) y eliminación controlada.
 *   Tras cada mutación exitosa dispara avisos al servicio externo de notificaciones.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Define y expone los contratos REST principales de gestión de
 *     vehículos, códigos HTTP semánticos (200, 201 + Location, 204) y validaciones.
 *   - [Actividad 3]: Atiende tráfico balanceado 3:1 desde NGINX en las instancias
 *     equivalentes A (:8080) y B (:8082) de forma 100% sin estado.
 *   - [Actividad 10 - P27]: Refleja y modifica el inventario compartido de vehículos
 *     con coherencia transversal entre ambas instancias.
 *   - [Actividad 11]: Protegido mediante matriz RBAC estricta en SecurityConfig:
 *     GET (CONSULTA/OPERADOR/ADMIN), POST/PUT/PATCH (OPERADOR/ADMIN), DELETE (ADMIN).
 */

// Define el paquete de controladores REST de la aplicación.
package ec.edu.uta.venta_vehiculos_api.controller;

// Importa URI para construir el encabezado HTTP Location al crear un recurso (201 Created).
import java.net.URI;
// Importa colecciones estándar (List, Map) utilizadas en firmas de endpoints.
import java.util.*;

// Importa @Valid para ejecutar las validaciones declarativas definidas en VehiculoRequest.
import jakarta.validation.Valid;

// Importa ResponseEntity para controlar códigos de estado y encabezados HTTP en las respuestas.
import org.springframework.http.ResponseEntity;
// Importa las anotaciones REST de Spring MVC para mapear rutas, verbos y parámetros.
import org.springframework.web.bind.annotation.*;

// Importa los DTOs de entrada y salida de vehículos.
import ec.edu.uta.venta_vehiculos_api.dto.*;
// Importa el servicio de negocio transaccional para vehículos.
import ec.edu.uta.venta_vehiculos_api.service.VehiculoService;
// Importa el cliente HTTP para enviar notificaciones de cambios al microservicio externo.
import ec.edu.uta.venta_vehiculos_api.client.NotificacionClient;

// [Actividad 1] Declara la clase como controlador REST que serializa respuestas directamente a JSON.
@RestController
// [Actividad 1] Establece el prefijo común /api/vehiculos para todos los endpoints de este controlador.
@RequestMapping("/api/vehiculos")
// Controlador principal de gestión del catálogo de vehículos.
public class VehiculoController {

    // Servicio inmutable que encapsula reglas de negocio, validaciones y persistencia de vehículos.
    private final VehiculoService service;

    // Cliente REST inmutable que despacha eventos al microservicio independiente de notificaciones (:8081).
    private final NotificacionClient avisos;

    // Constructor con inyección de dependencias de la capa de servicio y del cliente de notificaciones.
    public VehiculoController(
            // Servicio de gestión de inventario de vehículos.
            VehiculoService service,
            // Cliente HTTP encargado de enviar avisos por eventos de escritura.
            NotificacionClient avisos) {
        // Asigna el servicio de vehículos recibido.
        this.service = service;
        // Asigna el cliente de notificaciones recibido.
        this.avisos = avisos;
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Lista todos los vehículos o filtra por marca (?marca=...) para roles CONSULTA/OPERADOR/ADMIN.
    @GetMapping
    // Método GET /api/vehiculos con parámetro opcional marca.
    public List<VehiculoResponse> listar(
            // Lee el parámetro de consulta marca; si se omite llega como null y retorna todo el catálogo.
            @RequestParam(required = false) String marca) {
        // Delega la consulta de solo lectura a la capa de servicio.
        return service.listar(marca);
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Obtiene el detalle de un vehículo por su ID para roles CONSULTA/OPERADOR/ADMIN.
    @GetMapping("/{id}")
    // Método GET /api/vehiculos/{id} que retorna el vehículo o dispara 404 vía ApiError.
    public VehiculoResponse obtener(
            // Extrae el identificador numérico del vehículo desde la ruta.
            @PathVariable int id) {
        // Delega la búsqueda individual al servicio.
        return service.obtener(id);
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Crea un vehículo en estado DISPONIBLE (roles OPERADOR/ADMIN) y responde 201 Created.
    @PostMapping
    // Método POST /api/vehiculos que valida el cuerpo JSON de entrada.
    public ResponseEntity<VehiculoResponse> crear(
            // Deserializa y valida VehiculoRequest con Bean Validation (error 422 si falla).
            @Valid @RequestBody VehiculoRequest d) {
        // Persiste el nuevo vehículo en la base de datos compartida y recibe el DTO creado.
        var r = service.crear(d);

        // Notifica el alta del vehículo al microservicio externo sin comprometer la transacción ya confirmada.
        avisos.enviar(
                // ID recién generado para el vehículo.
                r.idVehiculo(),
                // Tipo de evento registrado.
                "REGISTRO",
                // Descripción del evento enviado en la notificación.
                "Vehículo registrado"
        );

        // Construye respuesta HTTP 201 Created incluyendo encabezado Location hacia el nuevo recurso y el DTO en el cuerpo.
        return ResponseEntity.created(
                // Genera el URI relativo /api/vehiculos/{idVehiculo}.
                URI.create("/api/vehiculos/" + r.idVehiculo())
        ).body(r);
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Actualiza integralmente un vehículo DISPONIBLE (roles OPERADOR/ADMIN).
    @PutMapping("/{id}")
    // Método PUT /api/vehiculos/{id} que reemplaza los atributos editables del vehículo.
    public VehiculoResponse actualizar(
            // Extrae el ID del vehículo a modificar desde la URL.
            @PathVariable int id,
            // Deserializa y valida todos los campos obligatorios del vehículo.
            @Valid @RequestBody VehiculoRequest d) {
        // Ejecuta la actualización bajo bloqueo pesimista FOR UPDATE en la capa de servicio.
        var r = service.actualizar(id, d);

        // Envía aviso de actualización al servicio de notificaciones.
        avisos.enviar(
                // ID del vehículo modificado.
                id,
                // Tipo de evento de actualización.
                "ACTUALIZACION",
                // Descripción textual del cambio.
                "Vehículo actualizado"
        );

        // Retorna HTTP 200 OK con el estado actualizado del vehículo.
        return r;
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Modifica parcialmente solo el precio usando RFC 7396 merge-patch (roles OPERADOR/ADMIN).
    @PatchMapping(
            // Ruta del recurso vehículo objetivo.
            value = "/{id}",
            // Exige explícitamente Content-Type: application/merge-patch+json.
            consumes = "application/merge-patch+json"
    )
    // Método PATCH /api/vehiculos/{id} que recibe un mapa JSON validado estrictamente en el servicio.
    public VehiculoResponse precio(
            // Extrae el ID del vehículo cuyo precio se actualizará.
            @PathVariable int id,
            // Recibe el objeto JSON parcial como mapa clave-valor para validar que solo contenga "precio".
            @RequestBody Map<String, Object> d) {
        // Ejecuta la validación y actualización del precio bajo bloqueo pesimista en la capa de servicio.
        var r = service.precio(id, d);

        // Envía notificación informando el ajuste de precio.
        avisos.enviar(
                // ID del vehículo afectado.
                id,
                // Categoría del aviso.
                "ACTUALIZACION",
                // Detalle específico de modificación de precio.
                "Precio actualizado"
        );

        // Retorna HTTP 200 OK con el vehículo actualizado.
        return r;
    }

    // [Actividad 1] [Actividad 10 - P27] [Actividad 11] Elimina un vehículo DISPONIBLE sin historial de reservas (exclusivo rol ADMIN).
    @DeleteMapping("/{id}")
    // Método DELETE /api/vehiculos/{id} que responde HTTP 204 No Content si la eliminación procede.
    public ResponseEntity<Void> eliminar(
            // Extrae el ID del vehículo que se desea eliminar.
            @PathVariable int id) {
        // Delega la verificación de disponibilidad, historial de reservas y borrado físico al servicio.
        service.eliminar(id);

        // Informa al microservicio de notificaciones que el vehículo fue eliminado.
        avisos.enviar(
                // ID del vehículo eliminado.
                id,
                // Tipo de evento de borrado.
                "ELIMINACION",
                // Mensaje descriptivo de la operación.
                "Vehículo eliminado"
        );

        // Devuelve HTTP 204 No Content confirmando la eliminación sin cuerpo de respuesta.
        return ResponseEntity.noContent().build();
    }

}
