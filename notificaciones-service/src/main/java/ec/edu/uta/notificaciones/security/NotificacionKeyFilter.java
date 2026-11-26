/**
 * ==============================================================================
 * ARCHIVO: NotificacionKeyFilter.java
 * COMPONENTE: notificaciones-service (Filtro de Seguridad HTTP)
 * ROL: Protege los metodos de escritura HTTP (como POST /api/notificaciones)
 *      exigiendo la cabecera X-Notificacion-Key validada en tiempo constante,
 *      mientras permite consultas de lectura GET y preflight OPTIONS sin clave
 *      interna para que el cliente JavaFX consulte el historial via NGINX.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 2]: Exige que la clave inyectada por configuracion tenga una
 *     entropia minima de 32 caracteres y nunca sea debil ni vacia.
 *   - [Actividad 8 y Actividad 9]: Habilita GET /api/notificaciones para JavaFX
 *     sin requerir la clave interna, y protege POST mediante comparacion en
 *     tiempo constante (MessageDigest.isEqual) resistente a timing attacks.
 *   - [Actividad 10 - P27]: Permite al cliente JavaFX consultar directamente
 *     el historial de ventas notificadas a traves del proxy NGINX.
 * ==============================================================================
 */
// Declara el paquete de seguridad del microservicio de notificaciones.
package ec.edu.uta.notificaciones.security;

// Importa IOException requerida por la firma de escritura de respuestas en filtros Servlet.
import java.io.IOException;
// Importa StandardCharsets para codificar de forma determinista las cadenas a bytes UTF-8.
import java.nio.charset.StandardCharsets;
// Importa MessageDigest para utilizar isEqual(), comparacion criptografica en tiempo constante.
import java.security.MessageDigest;
// Importa @Value para inyectar la propiedad externa app.notificacion-key desde configuracion o entorno.
import org.springframework.beans.factory.annotation.Value;
// Importa @Component para que Spring Boot registre automaticamente el filtro en la cadena Servlet.
import org.springframework.stereotype.Component;
// Importa OncePerRequestFilter para garantizar que la validacion se ejecute una sola vez por peticion HTTP.
import org.springframework.web.filter.OncePerRequestFilter;
// Importa las interfaces base de Jakarta Servlet (FilterChain, ServletException).
import jakarta.servlet.*;
// Importa las clases HTTP de Jakarta Servlet (HttpServletRequest, HttpServletResponse).
import jakarta.servlet.http.*;

/**
 * Filtro de autenticacion por clave compartida para operaciones de escritura HTTP.
 * Permite paso directo a GET y OPTIONS; rechaza con HTTP 401 (RFC 7807 problem+json)
 * cualquier escritura que no proporcione la cabecera X-Notificacion-Key valida.
 */
// Registra el filtro automaticamente en el contenedor web de Spring Boot.
@Component 
public class NotificacionKeyFilter extends OncePerRequestFilter {
    // Clave secreta esperada inyectada externamente y retenida de forma inmutable en memoria.
    private final String key;

    /**
     * Constructor que inyecta la clave compartida desde app.notificacion-key y
     * valida en tiempo de arranque que cumpla con la longitud minima de seguridad.
     *
     * @param key clave configurada mediante application.properties o variable NOTIFICACIONES_KEY.
     * @throws IllegalArgumentException si la clave tiene menos de 32 caracteres.
     */
    // [Actividad 2] Inyecta la clave desde configuracion externa evitando secretos codificados en fuente.
    public NotificacionKeyFilter(@Value("${app.notificacion-key}") String key) {
        // Verifica la politica minima de robustez (al menos 32 caracteres); falla rapido al arrancar si no cumple.
        if (key.length() < 32) throw new IllegalArgumentException("La clave no cumple los 32 caracteres");
        // Almacena la clave validada para comparaciones posteriores.
        this.key = key;
    }

    /**
     * Evalua cada solicitud HTTP entrante antes de llegar al controlador REST.
     *
     * @param req   peticion HTTP entrante.
     * @param res   respuesta HTTP saliente.
     * @param chain cadena de filtros subsiguientes.
     * @throws IOException      si falla la escritura de la respuesta de error.
     * @throws ServletException si ocurre un error en la cadena de filtros.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        // [Actividad 8, Actividad 9 y Actividad 10 - P27] Permitir solicitudes GET (consulta de historial para JavaFX y comprobaciones) y preflight OPTIONS sin exigir la clave interna.
        if ("GET".equalsIgnoreCase(req.getMethod()) || "OPTIONS".equalsIgnoreCase(req.getMethod())) {
            // Delega inmediatamente al siguiente filtro o controlador sin exigir X-Notificacion-Key.
            chain.doFilter(req, res);
            // Finaliza la ejecucion de este filtro para evitar continuar a la validacion de escritura.
            return;
        }

        // Extrae el valor enviado por el cliente en la cabecera personalizada X-Notificacion-Key.
        String h = req.getHeader("X-Notificacion-Key");

        // [Actividad 8 y Actividad 9] Compara ambos arreglos de bytes UTF-8 en tiempo constante (MessageDigest.isEqual) para mitigar ataques de temporizacion.
        // Si la cabecera esta ausente o no coincide para metodos de escritura (POST, etc.), rechaza con HTTP 401 Unauthorized.
        if (h == null || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8), h.getBytes(StandardCharsets.UTF_8))) {
            // Establece el codigo de estado HTTP 401 (No autorizado).
            res.setStatus(401);
            // Define el tipo de contenido estandar RFC 7807 para problemas estructurados en JSON.
            res.setContentType("application/problem+json");
            // Escribe el cuerpo JSON minimo sin filtrar detalles de la clave esperada.
            res.getWriter().write("{\"title\":\"Clave no válida\",\"status\":401}");
            // Detiene la cadena de filtros impidiendo el acceso al controlador.
            return;
        }

        // La clave coincide en tiempo constante: continua con la cadena de filtros hacia el controlador.
        chain.doFilter(req, res);
    }
}
