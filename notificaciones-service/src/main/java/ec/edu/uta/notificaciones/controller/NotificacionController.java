package ec.edu.uta.notificaciones.controller;

import ec.edu.uta.notificaciones.dto.NotificacionHistorialDto;
import ec.edu.uta.notificaciones.dto.NotificacionRequest;
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
import ec.edu.uta.notificaciones.service.NotificacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    /**
     * Endpoint GET para consultar el historial completo de notificaciones (Requerido para JavaFX).
     */
    @GetMapping
    public List<NotificacionHistorialDto> listar() {
        return notificacionService.obtenerHistorial();
    }

    /**
     * Endpoint POST para registrar avisos directos.
     */
    @PostMapping
    public ResponseEntity<NotificacionResponse> crear(
            @RequestBody NotificacionRequest request) {

        NotificacionResponse response =
                notificacionService.procesar(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
