package ec.edu.uta.monitor_balancer_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MonitorBalancerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorBalancerServiceApplication.class, args);
    }
}
