/**
 * ==============================================================================
 * ARCHIVO: WebConfig.java
 * COMPONENTE: monitor-balancer-service (Configuracion Web y CORS)
 * ROL: Configura las reglas CORS de solo lectura (GET y OPTIONS) sobre los
 *      endpoints /api/** del monitor para permitir consultas desde clientes web,
 *      dashboards o herramientas de verificacion sin bloqueos de origen cruzado.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11]: Facilita el consumo seguro y de solo lectura
 *     de los 5 endpoints /api/monitor/* desde clientes de observabilidad.
 * ==============================================================================
 */
// Declara el paquete de configuraciones de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.config;

// Importa @Configuration para registrar la configuracion MVC en Spring Boot.
import org.springframework.context.annotation.Configuration;
// Importa CorsRegistry para declarar politicas de intercambio de recursos de origen cruzado.
import org.springframework.web.servlet.config.annotation.CorsRegistry;
// Importa WebMvcConfigurer para personalizar la configuracion de Spring Web MVC.
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración general de la Web API de Monitoreo.
 * Habilita CORS para permitir que los endpoints del monitor puedan ser consultados
 * desde aplicaciones frontend, Postman o dashboards web sin bloqueos de origen cruzado.
 */
// Declara esta clase como configuracion Spring Web MVC.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Registra las reglas CORS de solo lectura para toda la superficie /api/**.
     *
     * @param registry registro de mapeos CORS administrado por Spring MVC.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Aplica la politica CORS unicamente a las rutas bajo el prefijo /api/**.
        registry.addMapping("/api/**")
                // Permite consultas desde cualquier origen cliente (dashboards locales, navegador, etc.).
                .allowedOrigins("*")
                // Restringe estrictamente los metodos permitidos a operaciones de solo lectura (GET y preflight OPTIONS).
                .allowedMethods("GET", "OPTIONS")
                // Acepta cualquier cabecera HTTP entrante en solicitudes de consulta.
                .allowedHeaders("*")
                // Cachea la respuesta preflight CORS durante 3600 segundos (1 hora) para reducir trafico extra.
                .maxAge(3600);
    }
}
