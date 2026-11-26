/*
 * ============================================================================
 * Archivo: Launcher.java
 * Proyecto: Cliente de Escritorio JavaFX (consultarvehiculojavafx) - SOA APE 04
 * Función:
 *   Punto de entrada auxiliar de la JVM que delega el arranque de la interfaz
 *   gráfica a la clase principal VehiculosApp (subclase de javafx.application.Application).
 *   Esta separación evita errores de carga de módulos JavaFX al ejecutar desde
 *   el classpath estándar o mediante Maven.
 * Actividades APE 04 cubiertas:
 *   - [Actividad 9]: Inicialización del cliente de escritorio JavaFX que se
 *     conecta exclusivamente vía NGINX (:8090) para consultar el catálogo de
 *     vehículos y monitorear notificaciones en vivo de RabbitMQ.
 * ============================================================================
 */
// Declara el paquete lógico al que pertenece esta clase lanzadora.
package ec.edu.com;

// Importa la clase base Application de JavaFX para invocar su método estático launch().
import javafx.application.Application;

// Define la clase pública y final (no heredable) que actúa como punto de entrada.
public final class Launcher {

    // Constructor privado para impedir la instanciación innecesaria de esta clase utilitaria.
    private Launcher(){}
    // Método principal estándar de Java invocado por la JVM al iniciar la aplicación de escritorio [Actividad 9].
    public static void main(String[] args){
        // Arranca el ciclo de vida de JavaFX delegando la creación de la ventana principal a VehiculosApp [Actividad 9].
        Application.launch(VehiculosApp.class, args);
    }
}
