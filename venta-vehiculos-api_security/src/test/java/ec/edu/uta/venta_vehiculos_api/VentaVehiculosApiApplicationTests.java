// Identifica el paquete de las pruebas de la aplicación.
package ec.edu.uta.venta_vehiculos_api;

// Importa la anotación que marca un método como prueba de JUnit.
import org.junit.jupiter.api.Test;
// Importa la anotación que inicia el contexto de Spring Boot en la prueba.
import org.springframework.boot.test.context.SpringBootTest;

// Configura la prueba para cargar el contexto completo de la aplicación.
@SpringBootTest
// Verifica que la configuración principal de la aplicación pueda inicializarse.
class VentaVehiculosApiApplicationTests {

	// Indica a JUnit que ejecute este método como una prueba.
	@Test
	void contextLoads() {
		// No requiere instrucciones: el éxito consiste en que el contexto haya cargado.
	}

}
