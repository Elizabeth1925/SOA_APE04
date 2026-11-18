package ec.edu.uta.venta_vehiculos_api.security;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

// Filtro de autenticación JWT que procesa el encabezado Authorization: Bearer <token>
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Servicio responsable de validar y extraer los claims del token JWT
    private final JwtService jwt;

    // Inyección de dependencias para JwtService
    public JwtAuthenticationFilter(JwtService jwt) { 
        this.jwt = jwt; 
    }

    // Método principal del filtro ejecutado en cada petición HTTP
    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res, 
            FilterChain chain)
            throws IOException, ServletException {

        // Extrae el encabezado Authorization
        String h = req.getHeader("Authorization");

        // Si no hay autenticación previa y se envía un Bearer token
        if (SecurityContextHolder.getContext().getAuthentication() == null &&
            h != null && h.startsWith("Bearer ")) {

            try {
                // Valida criptográficamente el token y obtiene sus claims
                var c = jwt.validarToken(h.substring(7));

                // Exige que el token sea específicamente de tipo access token (no refresh)
                if (!"access".equals(c.get("tipo", String.class))) {
                    throw new IllegalArgumentException("Se requiere access token");
                }

                // Obtiene y valida el rol del usuario contenido en los claims
                String rol = c.get("rol", String.class);
                if (!List.of("CONSULTA", "OPERADOR", "ADMIN").contains(rol)) {
                    throw new IllegalArgumentException("Rol inválido");
                }

                // Establece la autenticación en el SecurityContext con el rol asignado (ROLE_...)
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(
                        c.getSubject(), 
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + rol))
                    )
                );
            } catch (Exception ex) { 
                // En caso de token inválido o expirado, se limpia el contexto para impedir acceso no autorizado
                SecurityContextHolder.clearContext(); 
            }
        }

        // Pasa la solicitud al siguiente filtro en la cadena de seguridad
        chain.doFilter(req, res);
    }
}