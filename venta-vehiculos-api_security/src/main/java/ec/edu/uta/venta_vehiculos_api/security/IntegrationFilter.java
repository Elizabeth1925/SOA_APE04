package ec.edu.uta.venta_vehiculos_api.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

// Filtro de seguridad que intercepta peticiones internas entre servicios validando la clave compartida X-Service-Key
@Component
public class IntegrationFilter extends OncePerRequestFilter {

    // Clave secreta compartida inyectada desde application.properties
    private final String key;

    // Constructor que valida que la clave de integración posea una longitud segura mínima de 32 caracteres
    public IntegrationFilter(@Value("${app.security.ventas-key}") String key) {
        if (key.length() < 32) {
            throw new IllegalStateException("Clave de servicio corta");
        }
        this.key = key;
    }

    // Intercepta cada solicitud HTTP exactamente una vez
    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res, 
            FilterChain chain)
            throws IOException, ServletException {

        String path = req.getRequestURI();

        // Determina si la ruta solicitada corresponde a endpoints protegidos de reservas internas
        boolean interna = path.equals("/api/reservas") ||
                path.startsWith("/api/reservas/") ||
                path.matches("/api/vehiculos/[0-9]+/reservas");

        if (interna) {
            // Obtiene el encabezado con la clave de servicio
            String enviada = req.getHeader("X-Service-Key");

            // Compara en tiempo constante (MessageDigest.isEqual) para prevenir ataques de temporización (timing attacks)
            if (enviada == null || !MessageDigest.isEqual(
                    key.getBytes(StandardCharsets.UTF_8),
                    enviada.getBytes(StandardCharsets.UTF_8))) {

                // Si no coincide, rechaza con HTTP 401 Unauthorized y formato Problem Details JSON
                res.setStatus(401); 
                res.setContentType("application/problem+json");
                res.getWriter().write("{\"title\":\"Servicio no autorizado\",\"status\":401}");
                return;
            }

            // Si es válida, autentica la petición en el contexto de seguridad con el rol ROLE_SERVICIO_VENTAS
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                            "ventas-api", 
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICIO_VENTAS"))
                    )
            );
        }

        // Continúa con la cadena de filtros de seguridad
        chain.doFilter(req, res);
    }
}
