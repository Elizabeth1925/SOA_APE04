package ec.edu.uta.venta_vehiculos_api.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.service.ReservaService;

// Controlador REST que expone endpoints para la gestión de reservas de vehículos
@RestController
public class ReservaController {

    private final ReservaService service;

    // Inyección de dependencias del servicio de reservas
    public ReservaController(ReservaService service) {
        this.service = service;
    }

    // Endpoint POST para crear una reserva para el vehículo indicado por {id}
    @PostMapping("/api/vehiculos/{id}/reservas")
    public ReservaResponse reservar(
            @PathVariable int id,
            @Valid @RequestBody ReservaRequest d) {
        return service.reservar(id, d.operacionId());
    }

    // Endpoint GET para obtener los detalles de una reserva por su identificador UUID
    @GetMapping("/api/reservas/{id}")
    public ReservaResponse obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    // Endpoint GET para consultar una reserva a través de su parámetro ?operacionId=...
    @GetMapping("/api/reservas")
    public ReservaResponse operacion(@RequestParam String operacionId) {
        return service.porOperacion(operacionId);
    }

    // Endpoint PUT para confirmar una reserva existente convirtiéndola en venta
    @PutMapping("/api/reservas/{id}/confirmacion")
    public ReservaResponse confirmar(@PathVariable String id) {
        return service.confirmar(id);
    }

    // Endpoint DELETE para liberar/cancelar una reserva
    @DeleteMapping("/api/reservas/{id}")
    public ResponseEntity<Void> liberar(@PathVariable String id) {
        service.liberar(id);
        // Retorna HTTP 204 No Content confirmando la liberación
        return ResponseEntity.noContent().build();
    }
}
