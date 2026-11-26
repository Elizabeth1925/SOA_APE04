/**
 * ==============================================================================
 * ARCHIVO: NotificacionesServiceApplication.java
 * COMPONENTE: notificaciones-service (Spring Boot - Puerto 8081)
 * ROL: Punto de entrada principal que inicializa el contenedor de Spring Boot,
 *      el servidor embebido HTTP para /api/notificaciones y el consumidor AMQP
 *      conectado a RabbitMQ.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Arranque del microservicio desacoplado de
 *     notificaciones que escucha la cola durable notificaciones.ventas y expone
 *     el historial consultable por JavaFX.
 * ==============================================================================
 */
// Declara el paquete raiz del microservicio de notificaciones.
package ec.edu.uta.notificaciones;

// Importa el lanzador estatico que arranca el contexto de aplicacion Spring Boot.
import org.springframework.boot.SpringApplication;
// Importa la meta-anotacion que habilita configuracion automatica y escaneo de componentes.
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de arranque del microservicio notificaciones-service.
 * Activa el escaneo de componentes en ec.edu.uta.notificaciones.* (consumidor RabbitMQ,
 * controlador REST, filtro de seguridad, servicio de historial y manejador de excepciones).
 */
// [Actividad 8 y Actividad 9] Habilita autoconfiguracion de Spring Web y Spring AMQP (RabbitMQ) y escaneo de beans.
@SpringBootApplication
public class NotificacionesServiceApplication {

    /**
     * Metodo de entrada estandar de la JVM para iniciar el microservicio.
     *
     * @param args argumentos opcionales de linea de comandos reenviados a Spring Boot.
     */
    public static void main(String[] args) {
        // Inicializa el contexto de Spring, registra el filtro de clave, levanta Tomcat en :8081 y activa @RabbitListener.
        SpringApplication.run(NotificacionesServiceApplication.class, args);
    }
}
