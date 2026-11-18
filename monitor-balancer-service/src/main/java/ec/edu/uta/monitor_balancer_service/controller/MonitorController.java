package ec.edu.uta.monitor_balancer_service.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ec.edu.uta.monitor_balancer_service.dto.AlertaDto;
import ec.edu.uta.monitor_balancer_service.dto.BalanceoResponse;
import ec.edu.uta.monitor_balancer_service.dto.EstadisticasResponse;
import ec.edu.uta.monitor_balancer_service.dto.EstadoGeneralResponse;
import ec.edu.uta.monitor_balancer_service.dto.InstanciaInfo;
import ec.edu.uta.monitor_balancer_service.service.MonitorService;
import ec.edu.uta.monitor_balancer_service.service.NginxLogParserService;

@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService monitorService;
    private final NginxLogParserService logParserService;

    public MonitorController(MonitorService monitorService, NginxLogParserService logParserService) {
        this.monitorService = monitorService;
        this.logParserService = logParserService;
    }

    /**
     * Endpoint 1: Estado general del sistema.
     */
    @GetMapping("/estado")
    public ResponseEntity<EstadoGeneralResponse> obtenerEstado() {
        return ResponseEntity.ok(monitorService.consultarEstadoGeneral());
    }

    /**
     * Endpoint 2: Salud y latencias de instancias individuales.
     */
    @GetMapping("/instancias")
    public ResponseEntity<List<InstanciaInfo>> obtenerInstancias() {
        return ResponseEntity.ok(monitorService.consultarInstancias());
    }

    /**
     * Endpoint 3: Metricas de balanceo de carga real calculadas desde access.log.
     */
    @GetMapping("/balanceo")
    public ResponseEntity<BalanceoResponse> obtenerBalanceo() {
        return ResponseEntity.ok(logParserService.calcularBalanceo());
    }

    /**
     * Endpoint 4: Estadisticas cuantitativas de codigos HTTP y tiempos de respuesta.
     */
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasResponse> obtenerEstadisticas() {
        return ResponseEntity.ok(logParserService.calcularEstadisticas());
    }

    /**
     * Endpoint 5: Historial de los ultimos 20 eventos de alerta y transicion.
     */
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaDto>> obtenerAlertas() {
        return ResponseEntity.ok(monitorService.consultarAlertas());
    }
}
