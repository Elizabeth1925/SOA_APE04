package ec.edu.uta.venta_vehiculos_api.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// Clase de configuración global de Spring Security
@Configuration
public class SecurityConfig {

    // Desactiva el registro automático de JwtAuthenticationFilter como filtro de servlet estándar,
    // garantizando que se ejecute únicamente dentro de la cadena de Spring Security
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<JwtAuthenticationFilter>
    desactivarJwtServlet(JwtAuthenticationFilter f) {
        var b = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
        b.setEnabled(false); 
        return b;
    }

    // Desactiva el registro automático de IntegrationFilter en el contenedor de servlets
    @Bean
    public org.springframework.boot.web.servlet.FilterRegistrationBean<IntegrationFilter>
    desactivarIntegracionServlet(IntegrationFilter f) {
        var b = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(f);
        b.setEnabled(false); 
        return b;
    }

    // Publica el bean BCryptPasswordEncoder para hashear y verificar contraseñas
    @Bean 
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Construye la cadena principal de filtros de seguridad (SecurityFilterChain)
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwt, 
            IntegrationFilter integration)
            throws Exception {

        return http
            // Desactiva CSRF ya que la API es sin estado (stateless) y autenticada por tokens/claves
            .csrf(c -> c.disable())
            // Configura la política de sesiones como STATELESS (no se crean sesiones HTTP en servidor)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Manejo de errores de autenticación y autorización devolviendo Problem Details (RFC 7807)
            .exceptionHandling(e -> e
                .authenticationEntryPoint((q, r, x) -> {
                    r.setStatus(401); 
                    r.setContentType("application/problem+json");
                    r.getWriter().write("{\"title\":\"No autenticado\",\"status\":401}");
                })
                .accessDeniedHandler((q, r, x) -> {
                    r.setStatus(403); 
                    r.setContentType("application/problem+json");
                    r.getWriter().write("{\"title\":\"Sin permiso\",\"status\":403}");
                }))
            // Reglas de autorización de solicitudes HTTP por ruta, método y rol
            .authorizeHttpRequests(a -> a
                // Endpoint para obtener token OAuth2/JWT (público)
                .requestMatchers("/oauth/token").permitAll()
                // Endpoint de salud de Actuator para monitoreo y NGINX (público)
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                // Endpoints internos de reservas accesibles únicamente por el microservicio con clave compartida
                .requestMatchers("/api/reservas", "/api/reservas/**", "/api/vehiculos/*/reservas")
                    .hasRole("SERVICIO_VENTAS")
                // Endpoint de identidad accesible para cualquier rol autenticado
                .requestMatchers("/api/identidad")
                    .hasAnyRole("CONSULTA", "OPERADOR", "ADMIN")
                // Lectura de vehículos accesible para CONSULTA, OPERADOR y ADMIN
                .requestMatchers(HttpMethod.GET, "/api/vehiculos/**")
                    .hasAnyRole("CONSULTA", "OPERADOR", "ADMIN")
                // Creación, actualización completa y parcial accesibles para OPERADOR y ADMIN
                .requestMatchers(HttpMethod.POST, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/vehiculos/**")
                    .hasAnyRole("OPERADOR", "ADMIN")
                // Eliminación de vehículos reservada exclusivamente al rol ADMIN
                .requestMatchers(HttpMethod.DELETE, "/api/vehiculos/**")
                    .hasRole("ADMIN")
                // Cualquier otra solicitud no mapeada explícitamente es denegada
                .anyRequest().denyAll())
            // Orden de filtros: primero autenticación de integración interna, luego JWT, antes del filtro de usuario/contraseña
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(integration, JwtAuthenticationFilter.class)
            .build();
    }
}