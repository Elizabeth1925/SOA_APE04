package ec.edu.uta.monitor_balancer_service.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ec.edu.uta.monitor_balancer_service.client.HealthCheckClient;
import ec.edu.uta.monitor_balancer_service.dto.AlertaDto;
import ec.edu.uta.monitor_balancer_service.dto.EstadoGeneralResponse;
import ec.edu.uta.monitor_balancer_service.dto.InstanciaInfo;

@Service
public class MonitorService {

    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);
    private static final int MAX_ALERTAS = 20;

    private final HealthCheckClient client;

    @Value("${monitor.destinos.nginx.url}")
    private String nginxUrl;

    @Value("${monitor.destinos.instancia-a.url}")
    private String instanciaAUrl;

    @Value("${monitor.destinos.instancia-a.puerto:8080}")
    private int instanciaAPuerto;

    @Value("${monitor.destinos.instancia-b.url}")
    private String instanciaBUrl;

    @Value("${monitor.destinos.instancia-b.puerto:8082}")
    private int instanciaBPuerto;

    // Almacenamiento en memoria del ultimo estado conocido
    private final AtomicReference<EstadoGeneralResponse> ultimoEstado = new AtomicReference<>();

    // Estados previos para detectar transiciones
    private String previoNginx = null;
    private String previoInstanciaA = null;
    private String previoInstanciaB = null;
    private String previoEstadoGeneral = null;

    // Historial acotado a 20 alertas en memoria
    private final List<AlertaDto> historialAlertas = Collections.synchronizedList(new LinkedList<>());

    public MonitorService(HealthCheckClient client) {
        this.client = client;
    }

    /**
     * Tarea programada: se ejecuta periodicamente cada 5 segundos.
     * Mide salud, latencia, evalua reglas de estado agregado y registra transiciones.
     */
    @Scheduled(fixedRate = 5000)
    public void ejecutarSondeoPeriodico() {
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 1. Comprobacion de NGINX (/nginx-ping)
        HealthCheckClient.HealthResult resNginx = client.check(nginxUrl);
        String estadoNginx = resNginx.getEstado();

        // 2. Comprobacion de Instancia A (/actuator/health en 8080)
        HealthCheckClient.HealthResult resA = client.check(instanciaAUrl);
        String estadoA = resA.getEstado();
        InstanciaInfo infoA = new InstanciaInfo(instanciaAPuerto, instanciaAUrl, estadoA, resA.getLatenciaMs(), timestamp);

        // 3. Comprobacion de Instancia B (/actuator/health en 8082)
        HealthCheckClient.HealthResult resB = client.check(instanciaBUrl);
        String estadoB = resB.getEstado();
        InstanciaInfo infoB = new InstanciaInfo(instanciaBPuerto, instanciaBUrl, estadoB, resB.getLatenciaMs(), timestamp);

        List<InstanciaInfo> instancias = List.of(infoA, infoB);
        long upCount = (estadoA.equalsIgnoreCase("UP") ? 1 : 0) + (estadoB.equalsIgnoreCase("UP") ? 1 : 0);

        // 4. Reglas de Estado General Agregado (Tabla 2)
        String estadoGeneral;
        if (!"UP".equalsIgnoreCase(estadoNginx)) {
            estadoGeneral = "NGINX_DOWN";
        } else if (upCount == 2) {
            estadoGeneral = "NORMAL";
        } else if (upCount == 1) {
            estadoGeneral = "DEGRADADO";
        } else {
            estadoGeneral = "SIN_BACKENDS";
        }

        // 5. Deteccion de transiciones y registro de alertas (sin duplicados consecutivos)
        detectarTransicion("NGINX", previoNginx, estadoNginx, timestamp);
        detectarTransicion("Vehiculos_8080", previoInstanciaA, estadoA, timestamp);
        detectarTransicion("Vehiculos_8082", previoInstanciaB, estadoB, timestamp);
        detectarTransicion("ESTADO_GENERAL", previoEstadoGeneral, estadoGeneral, timestamp);

        // Actualizar estados previos
        this.previoNginx = estadoNginx;
        this.previoInstanciaA = estadoA;
        this.previoInstanciaB = estadoB;
        this.previoEstadoGeneral = estadoGeneral;

        EstadoGeneralResponse respuesta = new EstadoGeneralResponse(estadoNginx, estadoGeneral, instancias, timestamp);
        this.ultimoEstado.set(respuesta);

        log.info("[MONITOR] Sondeo cada 5s -> NGINX: {}, A(8080): {} ({}ms), B(8082): {} ({}ms) => ESTADO: {}",
                estadoNginx, estadoA, resA.getLatenciaMs(), estadoB, resB.getLatenciaMs(), estadoGeneral);
    }

    private void detectarTransicion(String componente, String previo, String nuevo, String timestamp) {
        if (previo != null && !previo.equalsIgnoreCase(nuevo)) {
            String mensaje = String.format("Transicion detectada en %s: %s -> %s", componente, previo, nuevo);
            AlertaDto alerta = new AlertaDto(timestamp, componente, previo, nuevo, mensaje);
            agregarAlerta(alerta);
            log.warn("[ALERTA] {}", mensaje);
        }
    }

    private synchronized void agregarAlerta(AlertaDto alerta) {
        historialAlertas.add(0, alerta); // insertar al inicio para ver las mas recientes primero
        while (historialAlertas.size() > MAX_ALERTAS) {
            historialAlertas.remove(historialAlertas.size() - 1);
        }
    }

    public EstadoGeneralResponse consultarEstadoGeneral() {
        EstadoGeneralResponse actual = ultimoEstado.get();
        if (actual == null) {
            ejecutarSondeoPeriodico();
            return ultimoEstado.get();
        }
        return actual;
    }

    public List<InstanciaInfo> consultarInstancias() {
        EstadoGeneralResponse actual = consultarEstadoGeneral();
        return actual != null ? actual.getInstancias() : Collections.emptyList();
    }

    public List<AlertaDto> consultarAlertas() {
        synchronized (historialAlertas) {
            return new ArrayList<>(historialAlertas);
        }
    }
}
