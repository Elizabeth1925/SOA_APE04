/**
 * ==============================================================================
 * ARCHIVO: AsyncSchedulingConfig.java
 * COMPONENTE: monitor-balancer-service (Configuracion de Planificacion de Tareas)
 * ROL: Define un pool dedicado de hilos para ejecutar las tareas @Scheduled del
 *      monitor (sondeos cada 5 segundos) sin bloquear los hilos de atencion de
 *      solicitudes HTTP en /api/monitor/*.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3 y Actividad 11 - P23/P25/P26]: Aislamiento de hilos para el
 *     sondeo periodico continuo de /nginx-ping y /actuator/health cada 5 s.
 * ==============================================================================
 */
// Declara el paquete de configuraciones tecnicas del servicio de monitoreo.
package ec.edu.uta.monitor_balancer_service.config;

// Importa @Bean para registrar el planificador en el contexto de Spring.
import org.springframework.context.annotation.Bean;
// Importa @Configuration para declarar esta clase como fuente de configuracion Spring.
import org.springframework.context.annotation.Configuration;
// Importa ThreadPoolTaskScheduler, implementacion de TaskScheduler basada en pool de hilos.
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración del planificador de tareas para las comprobaciones periódicas (@Scheduled).
 * Asigna un ThreadPool dedicado con hilos identificables ("monitor-scheduler-")
 * para aislar las revisiones de salud cada 5 segundos del hilo de atención HTTP.
 */
// Marca esta clase como clase de configuracion procesada al arranque de Spring Boot.
@Configuration
public class AsyncSchedulingConfig {

    /**
     * Crea y configura el bean ThreadPoolTaskScheduler usado por @Scheduled.
     *
     * @return planificador inicializado con 2 hilos de trabajo dedicados.
     */
    // Expone el planificador como bean "taskScheduler" reconocido automaticamente por Spring Scheduling.
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        // Instancia un nuevo planificador respaldado por un pool de hilos.
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // Configura 2 hilos concurrentes para evitar contencion si un sondeo espera hasta el timeout de 2 s.
        scheduler.setPoolSize(2);
        // Asigna un prefijo descriptivo al nombre de los hilos para facilitar la auditoria en logs.
        scheduler.setThreadNamePrefix("monitor-scheduler-");
        // Inicializa internamente el ScheduledThreadPoolExecutor subyacente.
        scheduler.initialize();
        // Devuelve el planificador listo para ser utilizado por MonitorService.
        return scheduler;
    }
}
