/**
 * Archivo: IntegrationFilter.java
 * Responsabilidad arquitectónica:
 *   Filtro HTTP de seguridad servicio-a-servicio (extiende OncePerRequestFilter) que
 *   intercepta exclusivamente los endpoints internos de reservas (/api/reservas,
 *   /api/reservas/** y /api/vehiculos/{id}/reservas) y autentica al orquestador
 *   ventas-api (FastAPI) mediante la cabecera compartida X-Service-Key validada en
 *   tiempo constante para prevenir ataques de temporización (timing attacks).
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Autentica las llamadas internas de la orquestación en 2 fases
 *     realizadas por ventas-api hacia ReservaController.
 *   - [Actividad 11]: Protege las rutas internas dentro de la arquitectura 100% STATELESS,
 *     emitiendo rechazos HTTP 401 en formato RFC 7807 (application/problem+json) si la
 *     clave falta o no coincide, y otorgando el rol interno ROLE_SERVICIO_VENTAS al
 *     SecurityContext cuando la verificación criptográfica en tiempo constante es exitosa.
 */

// Declara el paquete que concentra los componentes de seguridad del microservicio.
package ec.edu.uta.venta_vehiculos_api.security;

// Importa IOException para el manejo de escritura en la respuesta HTTP y cadena de filtros.
import java.io.IOException;
// Importa StandardCharsets.UTF_8 para convertir cadenas a arreglos de bytes de forma determinista.
import java.nio.charset.StandardCharsets;
// Importa MessageDigest para realizar comparaciones de secretos en tiempo constante (isEqual).
import java.security.MessageDigest;
// Importa List para construir la colección inmutable de autoridades asignadas al contexto.
import java.util.List;
// Importa @Value para inyectar la clave compartida externa sin quemarla en código fuente.
import org.springframework.beans.factory.annotation.Value;
// Importa UsernamePasswordAuthenticationToken para poblar el SecurityContext autenticado.
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// Importa SimpleGrantedAuthority para asignar la autoridad ROLE_SERVICIO_VENTAS.
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Importa SecurityContextHolder para almacenar la autenticación por hilo durante la solicitud actual.
import org.springframework.security.core.context.SecurityContextHolder;
// Importa @Component para registrar el filtro como bean de Spring (deshabilitado como servlet directo en SecurityConfig).
import org.springframework.stereotype.Component;
// Importa OncePerRequestFilter para garantizar una única ejecución del filtro por petición HTTP.
import org.springframework.web.filter.OncePerRequestFilter;
// Importa FilterChain y ServletException de Jakarta Servlet.
import jakarta.servlet.*;
// Importa HttpServletRequest y HttpServletResponse de Jakarta Servlet HTTP.
import jakarta.servlet.http.*;

// [Actividad 4] [Actividad 11] Registra el filtro de autenticación interna servicio-a-servicio como bean de Spring.
@Component
// Filtro que exige y valida X-Service-Key exclusivamente en los endpoints internos de reservas.
public class IntegrationFilter extends OncePerRequestFilter {

    // Clave secreta compartida con ventas-api (FastAPI), inyectada desde app.security.ventas-key.
    private final String key;

    // [Actividad 2] [Actividad 11] Constructor que verifica una longitud mínima segura de 32 caracteres al arrancar.
    public IntegrationFilter(@Value("${app.security.ventas-key}") String key) {
        // Impide el arranque del servicio si la clave de integración configurada es demasiado corta (fail-fast).
        if (key.length() < 32) {
            // Lanza IllegalStateException deteniendo la inicialización por configuración insegura.
            throw new IllegalStateException("Clave de servicio corta");
        }
        // Almacena la clave validada en el atributo inmutable.
        this.key = key;
    }

    // [Actividad 4] [Actividad 11] Ejecuta la validación de X-Service-Key exactamente una vez por cada petición HTTP entrante.
    @Override
    protected void doFilterInternal(
            // Petición HTTP entrante con URI y encabezados.
            HttpServletRequest req,
            // Respuesta HTTP saliente usada para responder 401 RFC 7807 ante rechazo.
            HttpServletResponse res, 
            // Cadena de filtros siguiente de Spring Security.
            FilterChain chain)
            throws IOException, ServletException {

        // Obtiene la ruta relativa solicitada (ej. /api/vehiculos/5/reservas o /api/reservas/...).
        String path = req.getRequestURI();

        // [Actividad 4] Identifica si la petición apunta a alguno de los endpoints internos de reservas.
        boolean interna = path.equals("/api/reservas") ||
                // Cubre /api/reservas/{id} y /api/reservas/{id}/confirmacion.
                path.startsWith("/api/reservas/") ||
                // Cubre POST /api/vehiculos/{id}/reservas para IDs numéricos.
                path.matches("/api/vehiculos/[0-9]+/reservas");

        // Solo aplica validación de clave interna cuando la ruta corresponde a reservas; el resto continúa hacia JwtAuthenticationFilter.
        if (interna) {
            // Lee el encabezado HTTP X-Service-Key enviado por ventas-api.
            String enviada = req.getHeader("X-Service-Key");

            // [Actividad 11] Compara bytes UTF-8 en tiempo constante con MessageDigest.isEqual para mitigar timing attacks.
            if (enviada == null || !MessageDigest.isEqual(
                    // Convierte la clave esperada a bytes UTF-8.
                    key.getBytes(StandardCharsets.UTF_8),
                    // Convierte la clave recibida en el encabezado a bytes UTF-8.
                    enviada.getBytes(StandardCharsets.UTF_8))) {

                // [Actividad 11] Rechaza inmediatamente con HTTP 401 Unauthorized y Content-Type RFC 7807.
                res.setStatus(401); 
                // Declara el tipo de contenido estándar Problem Details (RFC 7807).
                res.setContentType("application/problem+json");
                // Escribe el cuerpo JSON RFC 7807 indicando servicio no autorizado.
                res.getWriter().write("{\"title\":\"Servicio no autorizado\",\"status\":401}");
                // Cortocircuita la cadena de filtros sin invocar controladores ni JwtAuthenticationFilter.
                return;
            }

            // [Actividad 4] [Actividad 11] Registra la identidad interna "ventas-api" con autoridad ROLE_SERVICIO_VENTAS en el SecurityContext.
            SecurityContextHolder.getContext().setAuthentication(
                    // Crea el token de autenticación ya verificado (con autoridades y credenciales nulas).
                    new UsernamePasswordAuthenticationToken(
                            // Principal lógico que identifica al microservicio orquestador.
                            "ventas-api", 
                            // Sin credenciales en memoria tras la verificación.
                            null,
                            // Autoridad exigida por SecurityConfig en las rutas internas de reservas.
                            List.of(new SimpleGrantedAuthority("ROLE_SERVICIO_VENTAS"))
                    )
            );
        }

        // Continúa el procesamiento hacia el siguiente filtro de la cadena (JwtAuthenticationFilter).
        chain.doFilter(req, res);
    }
}
