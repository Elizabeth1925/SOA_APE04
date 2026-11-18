package ec.edu.uta.monitor_balancer_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuración del planificador de tareas para las comprobaciones periódicas (@Scheduled).
 * Asigna un ThreadPool dedicado con hilos identificables ("monitor-scheduler-")
 * para aislar las revisiones de salud cada 5 segundos del hilo de atención HTTP.
 */
@Configuration
public class AsyncSchedulingConfig {

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("monitor-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
