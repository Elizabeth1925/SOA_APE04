package ec.edu.uta.venta_vehiculos_api.controller;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// Controlador REST para inspeccionar la identidad y roles del usuario autenticado
@RestController
public class IdentidadController {

    // Retorna los datos de la autenticación actual (nombre de usuario y autoridades/roles concedidos)
    @GetMapping("/api/identidad")
    public Map<String, Object> identidad(Authentication a) {
        return Map.of(
            "usuario", a.getName(),
            "roles", a.getAuthorities().stream().map(x -> x.getAuthority()).toList()
        );
    }
}
