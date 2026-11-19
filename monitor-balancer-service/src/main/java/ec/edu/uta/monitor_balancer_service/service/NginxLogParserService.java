package ec.edu.uta.monitor_balancer_service.service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ec.edu.uta.monitor_balancer_service.dto.BalanceoResponse;
import ec.edu.uta.monitor_balancer_service.dto.EstadisticasResponse;

/**
 * Servicio encargado de parsear el archivo access.log de NGINX.
 * CORREGIDO (APE 04): Aplica ventana temporal real (por minutos o timestamp ISO-8601 de inicio),
 * garantizando que los calculos de balanceo y estadisticas cuantifiquen UNICAMENTE la corrida de prueba actual
 * y no incluyan peticiones residuales de ejecuciones anteriores.
 */
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
     * Ventana temporal por defecto en minutos (30 minutos para sesion de prueba activa).
     */
    @Value("${monitor.ventana.minutos-defecto:30}")
    private int ventanaMinutosDefecto;

    /**
     * Modelo interno que representa una linea parseada del access.log.
     */
    public static class LogEntry {
        public String timestamp;
        public OffsetDateTime parsedTime;
        public String method;
        public String uri;
        public int status;
        public String finalUpstream;
        public double requestTimeMs;

        public LogEntry(String timestamp, OffsetDateTime parsedTime, String method, String uri, int status, String finalUpstream, double requestTimeMs) {
            this.timestamp = timestamp;
            this.parsedTime = parsedTime;
            this.method = method;
            this.uri = uri;
            this.status = status;
            this.finalUpstream = finalUpstream;
            this.requestTimeMs = requestTimeMs;
        }
    }

    /**
     * Lee y parsea las lineas del access.log de NGINX aplicando una ventana temporal estricta.
     * @param ventanaMinutos minutos hacia atras a considerar (si es null, usa ventanaMinutosDefecto).
     * @param desdeIso timestamp ISO-8601 de inicio explicito (opcional).
     */
    public List<LogEntry> leerRegistros(Integer ventanaMinutos, String desdeIso) {
        List<LogEntry> entries = new ArrayList<>();
        File file = new File(logPath);
        if (!file.exists() || !file.canRead()) {
            log.warn("El archivo de logs de NGINX no existe o no se puede leer: {}", logPath);
            return entries;
        }

        // Determinar el limite temporal inferior
        OffsetDateTime limiteInferior = null;
        if (desdeIso != null && !desdeIso.trim().isEmpty()) {
            try {
                limiteInferior = OffsetDateTime.parse(desdeIso.trim());
            } catch (Exception ex) {
                log.warn("Formato invalido para parametro 'desde': {}. Ignorando.", desdeIso);
            }
        }
        if (limiteInferior == null) {
            int minutos = (ventanaMinutos != null && ventanaMinutos > 0) ? ventanaMinutos : ventanaMinutosDefecto;
            limiteInferior = OffsetDateTime.now().minusMinutes(minutos);
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Formato: time_iso8601|method|uri|status|upstream_addr|upstream_status|request_time|upstream_response_time
                String[] parts = line.split("\\|");
                if (parts.length < 8) {
                    continue;
                }

                String timestamp = parts[0];
                OffsetDateTime entryTime;
                try {
                    entryTime = OffsetDateTime.parse(timestamp);
                } catch (Exception ex) {
                    continue;
                }

                // FILTRO DE VENTANA TEMPORAL: Descartar lineas previas al inicio de la prueba
                if (entryTime.isBefore(limiteInferior)) {
                    continue;
                }

                String method = parts[1];
                String uri = parts[2];
                int status;
                try {
                    status = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    continue;
                }

                // Filtrar /nginx-ping y trafico ajeno al balanceo de negocio
                if (uri.startsWith("/nginx-ping")) {
                    continue;
                }

                // En caso de reintentos pasivos, NGINX concatena upstreams: "127.0.0.1:8082, 127.0.0.1:8080"
                String upstreamAddrRaw = parts[4].trim();
                String finalUpstream = "-";
                if (!upstreamAddrRaw.isEmpty() && !upstreamAddrRaw.equals("-")) {
                    String[] upstreams = upstreamAddrRaw.split(",\\s*");
                    finalUpstream = upstreams[upstreams.length - 1]; // ultimo upstream que efectivamente atendio
                }

                double requestTimeMs = 0.0;
                try {
                    requestTimeMs = Double.parseDouble(parts[6]) * 1000.0;
                } catch (NumberFormatException ignored) {}

                entries.add(new LogEntry(timestamp, entryTime, method, uri, status, finalUpstream, requestTimeMs));
            }
        } catch (Exception e) {
            log.error("Error al leer el archivo de logs de NGINX: {}", e.getMessage(), e);
        }

        return entries;
    }

    public List<LogEntry> leerRegistros() {
        return leerRegistros(null, null);
    }

    /**
     * Calcula metricas de balanceo aplicando ventana temporal real.
     */
    public BalanceoResponse calcularBalanceo(Integer ventanaMinutos, String desdeIso) {
        List<LogEntry> entries = leerRegistros(ventanaMinutos, desdeIso);
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

    public BalanceoResponse calcularBalanceo() {
        return calcularBalanceo(null, null);
    }

    /**
     * Calcula estadisticas cuantitativas aplicando ventana temporal real.
     */
    public EstadisticasResponse calcularEstadisticas(Integer ventanaMinutos, String desdeIso) {
        String criterio = (desdeIso != null && !desdeIso.trim().isEmpty())
                ? "Ventana explicita desde " + desdeIso.trim()
                : "Ventana de ultimos " + ((ventanaMinutos != null && ventanaMinutos > 0) ? ventanaMinutos : ventanaMinutosDefecto) + " minutos";

        List<LogEntry> entries = leerRegistros(ventanaMinutos, desdeIso);
        if (entries.isEmpty()) {
            return new EstadisticasResponse(null, null, 0, 0, 0, 0, 0.0, criterio);
        }

        String inicio = entries.get(0).timestamp;
        String fin = entries.get(entries.size() - 1).timestamp;

        long count2xx = 0;
        long count4xx = 0;
        long count5xx = 0;
        double sumaTiempo = 0.0;

        for (LogEntry e : entries) {
            int code = e.status;
            if (code >= 200 && code < 300) count2xx++;
            else if (code >= 400 && code < 500) count4xx++;
            else if (code >= 500 && code < 600) count5xx++;

            sumaTiempo += e.requestTimeMs;
        }

        long total = entries.size();
        double avgTiempo = total > 0 ? Math.round((sumaTiempo / total) * 100.0) / 100.0 : 0.0;

        return new EstadisticasResponse(inicio, fin, total, count2xx, count4xx, count5xx, avgTiempo, criterio);
    }

    public EstadisticasResponse calcularEstadisticas() {
        return calcularEstadisticas(null, null);
    }
}
