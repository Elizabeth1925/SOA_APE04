/**
 * Archivo: VentaVehiculosApiApplication.java
 * Responsabilidad arquitectónica:
 *   Clase principal de arranque (bootstrap) del microservicio Spring Boot
 *   venta-vehiculos-api_security. Inicializa el contenedor de inversión de control,
 *   la autoconfiguración de Spring Web MVC, Spring Data JPA, Spring Security y Actuator.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Inicializa los controladores REST, DTOs y modelos JPA del catálogo
 *     de vehículos y usuarios.
 *   - [Actividad 2]: Carga la configuración externa desde application.properties /
 *     variables de entorno sin secretos embebidos.
 *   - [Actividad 3]: Permite desplegar el mismo artefacto en dos instancias equivalentes
 *     (Instancia A :8080 e Instancia B :8082) detrás de NGINX en balanceo ponderado 3:1
 *     con Actuator habilitado para monitoreo.
 *   - [Actividad 4]: Registra los componentes de identidad y reservas en 2 fases con ventas-api.
 *   - [Actividad 11]: Inicializa la cadena de filtros STATELESS OAuth 2.0 / JWT y RBAC.
 */

// Define el paquete raíz desde el cual Spring Boot realiza el escaneo de componentes.
package ec.edu.uta.venta_vehiculos_api;

// Importa la utilidad de arranque que levanta el contexto embebido de Spring Boot.
import org.springframework.boot.SpringApplication;
// Importa la meta-anotación que combina @Configuration, @EnableAutoConfiguration y @ComponentScan.
import org.springframework.boot.autoconfigure.SpringBootApplication;

// [Actividad 2] [Actividad 3] Habilita autoconfiguración, escaneo de paquetes y registro de beans del microservicio.
@SpringBootApplication
// Declaración de la clase principal ejecutable del microservicio de vehículos.
public class VentaVehiculosApiApplication {

    // [Actividad 2] [Actividad 3] Punto de entrada estándar de la JVM para iniciar la Instancia A (:8080) o B (:8082).
    public static void main(String[] args) {
        // Arranca el contexto Spring Boot procesando argumentos CLI (por ejemplo, --server.port=8082) y propiedades externas.
        SpringApplication.run(VentaVehiculosApiApplication.class, args);
    }

}
