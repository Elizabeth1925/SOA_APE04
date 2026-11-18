package ec.edu.uta.venta_vehiculos_api.controller;

import java.net.URI;
import java.util.*;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.service.VehiculoService;
import ec.edu.uta.venta_vehiculos_api.client.NotificacionClient;

// Controlador REST que publica los endpoints para la gestión de vehículos
@RestController
// Ruta base para todas las operaciones sobre vehículos
@RequestMapping("/api/vehiculos")
public class VehiculoController {

    // Servicio que contiene la lógica de negocio y persistencia de vehículos
    private final VehiculoService service;

    // Cliente REST que envía avisos al microservicio independiente de notificaciones
    private final NotificacionClient avisos;

    // Inyección de dependencias mediante constructor para servicio y cliente de avisos
    public VehiculoController(
            VehiculoService service,
            NotificacionClient avisos) {
        this.service = service;
        this.avisos = avisos;
    }

    // Endpoint GET para listar todos los vehículos o filtrar opcionalmente por marca (?marca=...)
    @GetMapping
    public List<VehiculoResponse> listar(
            @RequestParam(required = false) String marca) {
        return service.listar(marca);
    }

    // Endpoint GET para consultar un vehículo específico por su identificador en la URL
    @GetMapping("/{id}")
    public VehiculoResponse obtener(
            @PathVariable int id) {
        return service.obtener(id);
    }

    // Endpoint POST para dar de alta un vehículo con validación de campos obligatorios
    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(
            @Valid @RequestBody VehiculoRequest d) {
        // Ejecuta la creación en la capa de servicio
        var r = service.crear(d);

        // Envía notificación asíncrona/remota informando el registro del nuevo vehículo
        avisos.enviar(
                r.idVehiculo(),
                "REGISTRO",
                "Vehículo registrado"
        );

        // Responde con código 201 Created y encabezado Location apuntando al recurso
        return ResponseEntity.created(
                URI.create("/api/vehiculos/" + r.idVehiculo())
        ).body(r);
    }

    // Endpoint PUT para actualizar completamente los datos de un vehículo existente
    @PutMapping("/{id}")
    public VehiculoResponse actualizar(
            @PathVariable int id,
            @Valid @RequestBody VehiculoRequest d) {
        // Actualiza el registro mediante la capa de servicio
        var r = service.actualizar(id, d);

        // Notifica el evento de actualización al servicio externo
        avisos.enviar(
                id,
                "ACTUALIZACION",
                "Vehículo actualizado"
        );

        return r;
    }

    // Endpoint PATCH para modificar únicamente el precio mediante application/merge-patch+json
    @PatchMapping(
            value = "/{id}",
            consumes = "application/merge-patch+json"
    )
    public VehiculoResponse precio(
            @PathVariable int id,
            @RequestBody Map<String, Object> d) {
        // Aplica el cambio parcial de precio
        var r = service.precio(id, d);

        // Notifica la modificación del precio
        avisos.enviar(
                id,
                "ACTUALIZACION",
                "Precio actualizado"
        );

        return r;
    }

    // Endpoint DELETE para remover un vehículo verificando reglas de negocio previas
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable int id) {
        // Solicita eliminar en el servicio
        service.eliminar(id);

        // Notifica el evento de eliminación
        avisos.enviar(
                id,
                "ELIMINACION",
                "Vehículo eliminado"
        );

        // Responde con código HTTP 204 No Content
        return ResponseEntity.noContent().build();
    }

}