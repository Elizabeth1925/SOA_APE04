/**
 * Archivo: SecurityConfig.java
 * Responsabilidad arquitectónica:
 *   Configuración central de Spring Security para todo el microservicio. Define una
 *   arquitectura 100% STATELESS (sin sesiones HTTP ni CSRF), registra el codificador
 *   BCrypt, desactiva el registro duplicado de filtros en el contenedor de servlets,
 *   configura respuestas de error 401/403 en formato RFC 7807 (application/problem+json),
 *   aplica una matriz RBAC estricta por ruta y verbo HTTP con denegación por defecto
 *   (anyRequest().denyAll()) y ordena los filtros IntegrationFilter y
 *   JwtAuthenticationFilter.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 3]: Expone públicamente GET /actuator/health y /actuator/health/**
 *     para que el monitor independiente (:8083) y el balanceador NGINX (ponderado 3:1
 *     entre Instancia A :8080 e Instancia B :8082) verifiquen la salud de cada nodo
 *     sin requerir credenciales, y mantiene SessionCreationPolicy.STATELESS para
 *     balanceo sin sticky sessions.
 *   - [Actividad 4]: Aísla los endpoints de reservas (/api/reservas, /api/reservas/**,
 *     /api/vehiculos/{id}/reservas) exclusivamente para ROLE_SERVICIO_VENTAS (ventas-api)
 *     y habilita /api/identidad para introspección de JWT desde ventas-api.
 *   - [Actividad 11]: Implementa la seguridad OAuth 2.0 / JWT STATELESS completa:
 *     BCryptPasswordEncoder, rechazos 401 y 403 RFC 7807, matriz RBAC estricta
 *     (CONSULTA solo lectura, OPERADOR lectura/escritura, ADMIN incluye DELETE),
 *     aislamiento de IntegrationFilter antes de JwtAuthenticationFilter y principio
 *     de mínimo privilegio con denyAll() para rutas no declaradas.
 */

// Declara el paquete que agrupa la configuración y filtros de seguridad.
package ec.edu.uta.venta_vehiculos_api.security;

// Importa @Configuration y @Bean para declarar la configuración Spring Security.
import org.springframework.context.annotation.*;
// Importa HttpMethod para discriminar reglas RBAC por verbo HTTP (GET, POST, PUT, PATCH, DELETE).
import org.springframework.http.HttpMethod;
// Importa HttpSecurity, constructor fluido de la cadena de filtros de Spring Security.
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// Importa SessionCreationPolicy para imponer modo 100% STATELESS.
import org.springframework.security.config.http.SessionCreationPolicy;
// Importa BCryptPasswordEncoder para hashing adaptativo con sal de contraseñas.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// Importa la interfaz PasswordEncoder inyectada en UsuarioApiService y LocalUsers.
import org.springframework.security.crypto.password.PasswordEncoder;
// Importa SecurityFilterChain que define la política efectiva de seguridad HTTP.
import org.springframework.security.web.SecurityFilterChain;
// Importa UsernamePasswordAuthenticationFilter como punto de anclaje en el orden de filtros.
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// [Actividad 3] [Actividad 4] [Actividad 11] Marca la clase como configuración central de Spring Security.
@Configuration
// Configura la cadena de filtros, codificador de contraseñas y autorización RBAC del microservicio.
public class SecurityConfig {

    // [Actividad 11] Evita que Spring Boot registre JwtAuthenticationFilter automáticamente como filtro servlet fuera de SecurityFilterChain.
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
    desactivarJwtServlet(JwtAuthenticationFilter f) {
        // Envuelve el filtro JWT en un FilterRegistrationBean para controlar su registro en Tomcat.
        var b = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
        // Deshabilita el registro automático para asegurar que se ejecute solo una vez dentro de Spring Security.
        b.setEnabled(false); 
        // Retorna el registro desactivado.
        return b;
    }

    // [Actividad 11] Evita el registro duplicado de IntegrationFilter en el contenedor de servlets fuera de SecurityFilterChain.
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<IntegrationFilter>
    desactivarIntegracionServlet(IntegrationFilter f) {
        // Envuelve el filtro de integración en un FilterRegistrationBean.
        var b = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
        // Deshabilita el registro servlet directo para respetar estrictamente el orden configurado en SecurityFilterChain.
        b.setEnabled(false); 
        // Retorna el registro desactivado.
        return b;
    }

    // [Actividad 11] Expone el bean BCryptPasswordEncoder para verificar y encriptar contraseñas de usuarios_api.
    @Bean 
    public PasswordEncoder passwordEncoder() {
        // Instancia BCrypt con factor de costo seguro por defecto.
        return new BCryptPasswordEncoder();
    }

    // [Actividad 3] [Actividad 4] [Actividad 11] Define la cadena de seguridad HTTP 100% STATELESS, errores RFC 7807 y matriz RBAC.
    @Bean
    public SecurityFilterChain securityFilterChain(
            // Constructor DSL de seguridad HTTP inyectado por Spring Security.
            HttpSecurity http,
            // Filtro STATELESS de validación de Access Token Bearer JWT.
            JwtAuthenticationFilter jwt, 
            // Filtro servicio-a-servicio de validación de X-Service-Key para ventas-api.
            IntegrationFilter integration)
            throws Exception {

        // Construye y devuelve el SecurityFilterChain con todas las políticas aplicadas.
        return http
            // [Actividad 11] Desactiva protección CSRF al no emplearse cookies ni sesiones de navegador (API REST con Bearer/X-Service-Key).
            .csrf(c -> c.disable())
            // [Actividad 3] [Actividad 11] Impone arquitectura 100% STATELESS: nunca se crea ni consulta HttpSession entre instancias A y B.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // [Actividad 11] Configura respuestas HTTP 401 y 403 en formato estándar RFC 7807 (application/problem+json).
            .exceptionHandling(e -> e
                // Manejador para solicitudes sin autenticación válida (sin token, token expirado o firma inválida): HTTP 401.
                .authenticationEntryPoint((q, r, x) -> {
                    // Establece estado HTTP 401 Unauthorized.
                    r.setStatus(401); 
                    // Establece tipo MIME RFC 7807 Problem Details.
                    r.setContentType("application/problem+json");
                    // Escribe el JSON estructurado de rechazo 401.
                    r.getWriter().write("{\"title\":\"No autenticado\",\"status\":401}");
                })
                // Manejador para usuarios autenticados cuyo rol RBAC no tiene privilegio sobre la ruta/verbo: HTTP 403.
                .accessDeniedHandler((q, r, x) -> {
                    // Establece estado HTTP 403 Forbidden.
                    r.setStatus(403); 
                    // Establece tipo MIME RFC 7807 Problem Details.
                    r.setContentType("application/problem+json");
                    // Escribe el JSON estructurado de rechazo 403.
                    r.getWriter().write("{\"title\":\"Sin permiso\",\"status\":403}");
                }))
            // [Actividad 3] [Actividad 4] [Actividad 11] Matriz estricta de autorización por ruta, verbo HTTP y rol.
            .authorizeHttpRequests(a -> a
                // [Actividad 11] Permite acceso público al endpoint OAuth 2.0 de obtención de tokens (valida Basic Auth + usuario internamente).
                .requestMatchers("/oauth/token").permitAll()
                // [Actividad 3] Expone públicamente GET /actuator/health y subrutas para monitoreo independiente (:8083) y salud de NGINX.
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                // [Actividad 4] [Actividad 11] Restringe las rutas internas de reservas exclusivamente al rol interno ROLE_SERVICIO_VENTAS.
                .requestMatchers("/api/reservas", "/api/reservas/**", "/api/vehiculos/*/reservas")
                    .hasRole("SERVICIO_VENTAS")
                // [Actividad 4] [Actividad 11] Habilita /api/identidad para cualquier rol JWT válido (consultado por ventas-api en orquestación).
                .requestMatchers("/api/identidad")
                    .hasAnyRole("CONSULTA", "OPERADOR", "ADMIN")
                // [Actividad 11] Autoriza lecturas GET de vehículos a los roles CONSULTA, OPERADOR y ADMIN.
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/**")
                    .hasAnyRole("CONSULTA", "OPERADOR", "ADMIN")
                // [Actividad 11] Autoriza creación POST de vehículos únicamente a OPERADOR y ADMIN (CONSULTA recibe 403).
                .requestMatchers(HttpMethod.POST, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                // [Actividad 11] Autoriza actualización total PUT de vehículos únicamente a OPERADOR y ADMIN.
                .requestMatchers(HttpMethod.PUT, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                // [Actividad 11] Autoriza ajuste parcial PATCH de precio únicamente a OPERADOR y ADMIN.
                .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                // [Actividad 11] Restringe la eliminación DELETE de vehículos exclusivamente al rol ADMIN (OPERADOR y CONSULTA reciben 403).
                .requestMatchers(HttpMethod.DELETE, "/api/vehiculos/**")
                    .hasRole("ADMIN")
                // [Actividad 11] Principio de denegación por defecto: cualquier ruta no declarada explícitamente queda bloqueada.
                .anyRequest().denyAll())
            // [Actividad 11] Inserta JwtAuthenticationFilter antes del filtro estándar de formulario de Spring Security.
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            // [Actividad 4] [Actividad 11] Inserta IntegrationFilter antes de JwtAuthenticationFilter para evaluar primero X-Service-Key en reservas.
            .addFilterBefore(integration, JwtAuthenticationFilter.class)
            // Compila y devuelve la cadena de filtros configurada.
            .build();
    }
}
