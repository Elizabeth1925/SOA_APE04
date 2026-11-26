/**
 * Archivo: VentaVehiculosApiApplicationTests.java
 * Responsabilidad arquitectónica:
 *   Prueba de humo (smoke test) basada en JUnit 5 y @SpringBootTest que verifica la
 *   correcta inicialización e inyección de dependencias del contexto completo de
 *   Spring Boot del microservicio venta-vehiculos-api_security.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 2]: Comprueba que la configuración externa y la estructura de beans
 *     permiten compilar y arrancar el contexto sin errores de cableado.
 *   - [Actividad 3]: Valida la integridad base del artefacto desplegable en Instancia A
 *     (:8080) e Instancia B (:8082).
 *   - [Actividad 11]: Verifica que los beans de seguridad STATELESS (SecurityConfig,
 *     JwtService, JwtAuthenticationFilter e IntegrationFilter) se registren sin conflictos.
 */

// Identifica el paquete raíz de pruebas alineado con el paquete principal de la aplicación.
package ec.edu.uta.venta_vehiculos_api;

// Importa @Test de JUnit Jupiter (JUnit 5) para declarar casos de prueba ejecutables.
import org.junit.jupiter.api.Test;
// Importa @SpringBootTest para arrancar el ApplicationContext completo durante la prueba.
import org.springframework.boot.test.context.SpringBootTest;

// [Actividad 2] [Actividad 11] Instruye a Spring Test a cargar la configuración integral del microservicio.
@SpringBootTest
// Clase de prueba de humo encargada de verificar el arranque del contexto Spring Boot.
class VentaVehiculosApiApplicationTests {

	// Declara el método de prueba de carga de contexto ejecutable por JUnit 5.
	@Test
	void contextLoads() {
		// Sin aserciones adicionales: la prueba pasa si el ApplicationContext inicializa sin lanzar excepciones.
	}

}
