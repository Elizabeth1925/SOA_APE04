/**
 * Archivo: JwtAuthenticationFilter.java
 * Responsabilidad arquitectónica:
 *   Filtro de autenticación HTTP 100% STATELESS (extiende OncePerRequestFilter) que
 *   inspecciona el encabezado "Authorization: Bearer <jwt>", verifica la firma
 *   criptográfica HMAC-SHA y vigencia del token vía JwtService, exige que el claim
 *   "tipo" sea "access" (bloqueando el uso de refresh tokens en recursos protegidos)
 *   y valida que el rol pertenezca estrictamente a CONSULTA, OPERADOR o ADMIN.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 3]: Permite autenticar peticiones sin afinidad de sesión (sticky
 *     sessions) en cualquiera de las instancias equivalentes A (:8080) o B (:8082)
 *     detrás de NGINX en balanceo ponderado 3:1.
 *   - [Actividad 11]: Pilar de la seguridad OAuth 2.0 / JWT STATELESS; reconstruye
 *     el SecurityContext en cada petición con el rol RBAC (ROLE_CONSULTA,
 *     ROLE_OPERADOR o ROLE_ADMIN) o limpia el contexto ante tokens alterados/expirados
 *     para que SecurityConfig responda 401 RFC 7807.
 */

// Declara el paquete que contiene los componentes de seguridad y JWT.
package ec.edu.uta.venta_vehiculos_api.security;

// Importa IOException requerida por la firma de doFilterInternal.
import java.io.IOException;
// Importa List para validar la lista blanca de roles permitidos y construir autoridades.
import java.util.List;
// Importa UsernamePasswordAuthenticationToken para poblar el SecurityContext autenticado.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Importa SimpleGrantedAuthority para mapear el rol del JWT al prefijo ROLE_ usado por Spring Security.
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Importa SecurityContextHolder para establecer o limpiar la autenticación de la petición actual.
import org.springframework.security.core.context.SecurityContextHolder;
// Importa @Component para registrar este filtro como bean de Spring (desactivado como filtro servlet en SecurityConfig).
import org.springframework.stereotype.Component;
// Importa OncePerRequestFilter para ejecutar la validación JWT una sola vez por solicitud HTTP.
import org.springframework.web.filter.OncePerRequestFilter;
// Importa FilterChain y ServletException de Jakarta Servlet.
import jakarta.servlet.*;
// Importa HttpServletRequest y HttpServletResponse de Jakarta Servlet HTTP.
import jakarta.servlet.http.*;

// [Actividad 3] [Actividad 11] Registra el filtro JWT STATELESS para su inserción en SecurityFilterChain.
@Component
// Filtro encargado de autenticar cada petición HTTP que porte un Access Token Bearer válido.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Servicio inmutable que verifica la firma HMAC-SHA, expiración y estructura del JWT.
    private final JwtService jwt;

    // Constructor con inyección de dependencias de JwtService.
    public JwtAuthenticationFilter(JwtService jwt) { 
        // Guarda la referencia del servicio criptográfico JWT.
        this.jwt = jwt; 
    }

    // [Actividad 3] [Actividad 11] Intercepta cada solicitud HTTP para validar el Bearer token sin usar sesiones en servidor.
    @Override
    protected void doFilterInternal(
            // Petición HTTP entrante desde NGINX o directamente desde el cliente.
            HttpServletRequest req,
            // Respuesta HTTP saliente propagada a lo largo de la cadena.
            HttpServletResponse res, 
            // Siguiente eslabón en la cadena de filtros de Spring Security.
            FilterChain chain)
            throws IOException, ServletException {

        // Lee el encabezado HTTP Authorization si fue enviado por el cliente.
        String h = req.getHeader("Authorization");

        // [Actividad 11] Solo autentica vía JWT si no hay ya una autenticación previa (ej. IntegrationFilter) y el esquema es "Bearer ".
        if (SecurityContextHolder.getContext().getAuthentication() == null &&
            h != null && h.startsWith("Bearer ")) {

            // Bloque protegido para capturar cualquier fallo de firma, expiración, formato o claims inválidos.
            try {
                // [Actividad 3] [Actividad 11] Remueve el prefijo "Bearer " (7 caracteres) y valida firma HMAC-SHA compartida + expiración.
                var c = jwt.validarToken(h.substring(7));

                // [Actividad 11] Exige explícitamente que el JWT sea de tipo "access" (rechaza refresh tokens en endpoints protegidos).
                if (!"access".equals(c.get("tipo", String.class))) {
                    // Interrumpe la autenticación forzando la limpieza del contexto en el bloque catch.
                    throw new IllegalArgumentException("Se requiere access token");
                }

                // [Actividad 11] Extrae el claim "rol" y comprueba contra lista blanca estricta de roles RBAC admitidos.
                String rol = c.get("rol", String.class);
                // Verifica que el rol pertenezca exclusivamente a CONSULTA, OPERADOR o ADMIN.
                if (!List.of("CONSULTA", "OPERADOR", "ADMIN").contains(rol)) {
                    // Rechaza roles manipulados o desconocidos forzando la limpieza del contexto.
                    throw new IllegalArgumentException("Rol inválido");
                }

                // [Actividad 11] Pobla el SecurityContext STATELESS con el usuario (sub) y la autoridad ROLE_<rol>.
                SecurityContextHolder.getContext().setAuthentication(
                    // Construye el objeto de autenticación reconocido por las reglas .hasRole / .hasAnyRole de SecurityConfig.
                    new UsernamePasswordAuthenticationToken(
                        // Nombre del usuario autenticado extraído del claim estándar "sub".
                        c.getSubject(), 
                        // Credenciales en null al haberse completado la verificación criptográfica del JWT.
                        null,
                        // Convierte el rol (ej. ADMIN) en la autoridad Spring Security (ej. ROLE_ADMIN).
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                    )
                );
            // Captura excepciones de JJWT (firma inválida, token expirado, mal formado) o validaciones de claims.
            } catch (Exception ex) { 
                // [Actividad 11] Limpia el SecurityContext para que SecurityConfig emita HTTP 401 RFC 7807 si la ruta es protegida.
                SecurityContextHolder.clearContext(); 
            }
        }

        // Continúa la ejecución hacia el siguiente filtro y las reglas de autorización de SecurityConfig.
        chain.doFilter(req, res);
    }
}
