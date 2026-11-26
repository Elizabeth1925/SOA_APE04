/**
 * Archivo: IdentidadController.java
 * Responsabilidad arquitectónica:
 *   Controlador REST que expone el endpoint de introspección de identidad
 *   GET /api/identidad para consultar el usuario autenticado y sus roles efectivos
 *   a partir del SecurityContext poblado por el token JWT.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Endpoint invocado por ventas-api (FastAPI) durante la orquestación
 *     en 2 fases para delegar la validación del token Bearer del cliente y verificar
 *     su identidad y rol autorizado sin compartir el secreto JWT con FastAPI.
 *   - [Actividad 11]: Refleja la identidad y autoridades RBAC (ROLE_CONSULTA,
 *     ROLE_OPERADOR, ROLE_ADMIN) reconstruidas de forma 100% STATELESS por
 *     JwtAuthenticationFilter.
 */

// Define el paquete de controladores REST del microservicio.
package ec.edu.uta.venta_vehiculos_api.controller;

// Importa Map para construir la respuesta JSON inmutable con usuario y roles.
import java.util.Map;
// Importa Authentication de Spring Security, inyectada automáticamente desde el SecurityContext actual.
import org.springframework.security.core.Authentication;
// Importa las anotaciones Web MVC (@RestController, @GetMapping, etc.).
import org.springframework.web.bind.annotation.*;

// [Actividad 4] [Actividad 11] Declara este controlador como proveedor de respuestas JSON REST.
@RestController
// Controlador que expone los datos de identidad del sujeto autenticado mediante JWT.
public class IdentidadController {

    // [Actividad 4] [Actividad 11] Expone GET /api/identidad (accesible a roles CONSULTA, OPERADOR y ADMIN).
    @GetMapping("/api/identidad")
    // Recibe el objeto Authentication creado por JwtAuthenticationFilter y devuelve usuario y lista de roles.
    public Map<String, Object> identidad(Authentication a) {
        // Construye y retorna un mapa JSON inmutable con el nombre de usuario y sus autoridades concedidas.
        return Map.of(
            // Campo "usuario": corresponde al claim subject ("sub") extraído del Access Token JWT.
            "usuario", a.getName(),
            // Campo "roles": transforma cada GrantedAuthority (ej. "ROLE_OPERADOR") a una lista de cadenas.
            "roles", a.getAuthorities().stream().map(x -> x.getAuthority()).toList()
        );
    }
}
