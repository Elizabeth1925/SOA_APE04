package ec.edu.uta.monitor_balancer_service.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ec.edu.uta.monitor_balancer_service.dto.BalanceoResponse;
import ec.edu.uta.monitor_balancer_service.dto.EstadisticasResponse;

@Service
public class NginxLogParserService {

    private static final Logger log = LoggerFactory.getLogger(NginxLogParserService.class);

    @Value("${monitor.nginx.log-path:C:/nginx/logs/access.log}")
    private String logPath;

    @Value("${monitor.destinos.instancia-a.puerto:8080}")
    private int puertoA;

    @Value("${monitor.destinos.instancia-b.puerto:8082}")
    private int puertoB;

    /**
     * Modelo interno que representa una linea parseada del access.log.
     */
    public static class LogEntry {
        String timestamp;
        String method;
        String uri;
        int status;
        String finalUpstream;
        double requestTimeMs;

        public LogEntry(String timestamp, String method, String uri, int status, String finalUpstream, double requestTimeMs) {
            this.timestamp = timestamp;
            this.method = method;
            this.uri = uri;
            this.status = status;
            this.finalUpstream = finalUpstream;
            this.requestTimeMs = requestTimeMs;
        }
    }

    /**
     * Lee y parsea las lineas del access.log de NGINX aplicando filtros y manejo de reintentos.
     */
    public List<LogEntry> leerRegistros() {
        List<LogEntry> entries = new ArrayList<>();
        File file = new File(logPath);
        if (!file.exists() || !file.canRead()) {
            log.warn("El archivo de logs de NGINX no existe o no se puede leer: {}", logPath);
            return entries;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Formato: time_iso8601|method|uri|status|upstream_addr|upstream_status|request_time|upstream_response_time
                String[] parts = line.split("\\|");
                if (parts.length < 8) {
                    continue; // Formato anterior o linea no conforme
                }

                String timestamp = parts[0];
                String method = parts[1];
                String uri = parts[2];
                int status;
                try {
                    status = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    continue;
                }

                // Filtrar /nginx-ping y trafico que no corresponda al balanceo
                if (uri.startsWith("/nginx-ping")) {
                    continue;
                }

                // En caso de reintentos pasivos, NGINX concatena upstreams con coma: "127.0.0.1:8082, 127.0.0.1:8080"
                // La guia exige contabilizar la instancia que finalmente atendio la solicitud.
                String upstreamAddrRaw = parts[4].trim();
                String finalUpstream = "-";
                if (!upstreamAddrRaw.isEmpty() && !upstreamAddrRaw.equals("-")) {
                    String[] upstreams = upstreamAddrRaw.split(",\\s*");
                    finalUpstream = upstreams[upstreams.length - 1]; // ultimo upstream de la cadena
                }

                double requestTimeMs = 0.0;
                try {
                    // request_time en segundos con decimales
                    requestTimeMs = Double.parseDouble(parts[6]) * 1000.0;
                } catch (NumberFormatException ignored) {}

                entries.add(new LogEntry(timestamp, method, uri, status, finalUpstream, requestTimeMs));
            }
        } catch (Exception e) {
            log.error("Error al leer el archivo de logs de NGINX: {}", e.getMessage(), e);
        }

        return entries;
    }

    /**
     * Calcula metricas de balanceo (Actividad 6 y 7).
     */
    public BalanceoResponse calcularBalanceo() {
        List<LogEntry> entries = leerRegistros();
        if (entries.isEmpty()) {
            return new BalanceoResponse(null, null, 0, 0, 0, 0, 0.0, 0.0);
        }

        String inicio = entries.get(0).timestamp;
        String fin = entries.get(entries.size() - 1).timestamp;

        long solicitudesA = 0;
        long solicitudesB = 0;
        long sinUpstream = 0;

        String patternA = ":" + puertoA;
        String patternB = ":" + puertoB;

        for (LogEntry e : entries) {
            if (e.finalUpstream.contains(patternA)) {
                solicitudesA++;
            } else if (e.finalUpstream.contains(patternB)) {
                solicitudesB++;
            } else {
                sinUpstream++;
            }
        }

        long total = entries.size();
        double porcentajeA = total > 0 ? Math.round((solicitudesA * 100.0 / total) * 100.0) / 100.0 : 0.0;
        double porcentajeB = total > 0 ? Math.round((solicitudesB * 100.0 / total) * 100.0) / 100.0 : 0.0;

        return new BalanceoResponse(inicio, fin, total, solicitudesA, solicitudesB, sinUpstream, porcentajeA, porcentajeB);
    }

    /**
     * Calcula estadisticas de codigos HTTP y tiempos de respuesta (Actividad 6 y 7).
     */
    public EstadisticasResponse calcularEstadisticas() {
        List<LogEntry> entries = leerRegistros();
        if (entries.isEmpty()) {
            return new EstadisticasResponse(null, null, 0, 0, 0, 0, 0.0, "Ventana completa de logs");
        }

        String inicio = entries.get(0).timestamp;
        String fin = entries.get(entries.size() - 1).timestamp;

        long c2xx = 0;
        long c4xx = 0;
        long c5xx = 0;
        double sumaTiempos = 0.0;

        for (LogEntry e : entries) {
            if (e.status >= 200 && e.status < 300) c2xx++;
            else if (e.status >= 400 && e.status < 500) c4xx++;
            else if (e.status >= 500 && e.status < 600) c5xx++;

            sumaTiempos += e.requestTimeMs;
        }

        long total = entries.size();
        double promedioMs = total > 0 ? Math.round((sumaTiempos / total) * 100.0) / 100.0 : 0.0;

        return new EstadisticasResponse(inicio, fin, total, c2xx, c4xx, c5xx, promedioMs, "Todas las solicitudes de la ventana activa de logs NGINX");
    }
}
