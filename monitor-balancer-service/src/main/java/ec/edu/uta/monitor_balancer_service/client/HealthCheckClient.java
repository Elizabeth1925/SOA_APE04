package ec.edu.uta.monitor_balancer_service.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Componente tecnico para las peticiones HTTP de sondeo de salud.
 * Configura timeouts finitos de conexion y lectura para evitar bloqueos indefinidos.
 * CORREGIDO (APE 04): Valida el contenido de la respuesta de Actuator exigiendo
 * explicitamente status "UP" en el cuerpo JSON, evitando interpretar cualquier 2xx como saludable.
 */
@Component
public class HealthCheckClient {

    private final HttpClient httpClient;
    private final Duration readTimeout;

    public HealthCheckClient(
            @Value("${monitor.client.connect-timeout-ms:2000}") long connectTimeoutMs,
            @Value("${monitor.client.read-timeout-ms:2000}") long readTimeoutMs) {
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    /**
     * Realiza una consulta GET sincrona con timeout estricto y validacion de Actuator.
     */
    public HealthResult check(String url) {
        long start = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(this.readTimeout)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = Math.max(1, System.currentTimeMillis() - start);
            int statusCode = response.statusCode();
            String body = response.body();

            // Validacion de Actuator: El cuerpo JSON debe contener explicitamente "status":"UP"
            boolean actuatorUp = body != null && (
                    body.contains("\"status\":\"UP\"") ||
                    body.contains("\"status\": \"UP\"") ||
                    body.toUpperCase().contains("\"STATUS\":\"UP\"") ||
                    body.toUpperCase().contains("\"STATUS\": \"UP\"")
            );

            if (statusCode >= 200 && statusCode < 300) {
                if (actuatorUp) {
                    return new HealthResult("UP", latency, statusCode, null);
                } else {
                    return new HealthResult("DOWN", latency, statusCode,
                            "Respuesta HTTP " + statusCode + " pero Actuator no reporta status UP");
                }
            } else if (statusCode == 401 || statusCode == 403) {
                return new HealthResult("ERROR_ACCESO", latency, statusCode, "Error de autorizacion HTTP " + statusCode);
            } else {
                return new HealthResult("DOWN", latency, statusCode, "Respuesta HTTP inesperada " + statusCode);
            }
        } catch (Exception ex) {
            // SocketTimeoutException, ConnectException, etc.
            return new HealthResult("DOWN", null, null, ex.getMessage());
        }
    }

    public static class HealthResult {
        private final String estado; // UP, DOWN, ERROR_ACCESO
        private final Long latenciaMs;
        private final Integer httpStatus;
        private final String mensaje;

        public HealthResult(String estado, Long latenciaMs, Integer httpStatus, String mensaje) {
            this.estado = estado;
            this.latenciaMs = latenciaMs;
            this.httpStatus = httpStatus;
            this.mensaje = mensaje;
        }

        public String getEstado() {
            return estado;
        }

        public Long getLatenciaMs() {
            return latenciaMs;
        }

        public Integer getHttpStatus() {
            return httpStatus;
        }

        public String getMensaje() {
            return mensaje;
        }
    }
}
