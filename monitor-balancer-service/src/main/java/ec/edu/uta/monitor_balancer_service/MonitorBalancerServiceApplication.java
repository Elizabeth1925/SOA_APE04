/**
 * ==============================================================================
 * ARCHIVO: MonitorBalancerServiceApplication.java
 * COMPONENTE: monitor-balancer-service (Spring Boot - Puerto 8083)
 * ROL: Clase principal que inicia el microservicio independiente de monitoreo
 *      y habilita la ejecucion periodica de tareas (@EnableScheduling) para los
 *      sondeos de salud cada 5 segundos.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P23/P24/P25/P26]: Arranque del servicio de
 *     observabilidad en puerto 8083 completamente desacoplado del flujo
 *     transaccional principal.
 * ==============================================================================
 */
// Declara el paquete raiz del servicio monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service;

// Importa el lanzador estatico de aplicaciones Spring Boot.
import org.springframework.boot.SpringApplication;
// Importa la meta-anotacion que activa autoconfiguracion y escaneo de beans en el paquete base.
import org.springframework.boot.autoconfigure.SpringBootApplication;
// Importa la anotacion que habilita el soporte para metodos anotados con @Scheduled.
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de monitor-balancer-service.
 * Activa el servidor HTTP en :8083 y el planificador dedicado definido en AsyncSchedulingConfig.
 */
// Habilita autoconfiguracion de Spring Web y deteccion de controladores, servicios y clientes HTTP.
@SpringBootApplication
// [Actividad 3] Habilita la programacion periodica requerida por MonitorService.ejecutarSondeoPeriodico() cada 5 segundos.
@EnableScheduling
public class MonitorBalancerServiceApplication {

    /**
     * Metodo principal de arranque de la aplicacion Java.
     *
     * @param args argumentos opcionales recibidos desde linea de comandos.
     */
    public static void main(String[] args) {
        // Inicializa el contexto Spring Boot, levanta la API /api/monitor/* en :8083 y dispara el scheduler cada 5s.
        SpringApplication.run(MonitorBalancerServiceApplication.class, args);
    }
}
