package ec.edu.uta.notificaciones.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

@Component 
public class NotificacionKeyFilter extends OncePerRequestFilter {
    private final String key;

    public NotificacionKeyFilter(@Value("${app.notificacion-key}") String key) {
        if (key.length() < 32) throw new IllegalArgumentException("La clave no cumple los 32 caracteres");
        this.key = key;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        // Permitir solicitudes GET (consulta de historial para JavaFX y comprobaciones) sin exigir la clave interna
        if ("GET".equalsIgnoreCase(req.getMethod()) || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String h = req.getHeader("X-Notificacion-Key");

        // Si no coincide para metodos de escritura (POST, etc.), rechaza con HTTP 401 Unauthorized
        if (h == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), h.getBytes(StandardCharsets.UTF_8))) {
            res.setStatus(401);
            res.setContentType("application/problem+json");
            res.getWriter().write("{\"title\":\"Clave no válida\",\"status\":401}");
            return;
        }

        // Continua con la cadena de filtros
        chain.doFilter(req, res);
    }
}
