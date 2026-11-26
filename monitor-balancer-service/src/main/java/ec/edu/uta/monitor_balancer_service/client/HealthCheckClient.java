/**
 * ==============================================================================
 * ARCHIVO: HealthCheckClient.java
 * COMPONENTE: monitor-balancer-service (Cliente HTTP de Sondeo de Salud)
 * ROL: Ejecuta peticiones HTTP GET sincronas con timeouts finitos hacia
 *      /nginx-ping (:8090) y /actuator/health (:8080 y :8082), mide latencia en
 *      milisegundos y valida explicitamente que el cuerpo JSON reporte
 *      {"status":"UP"}.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3]: Sondeo robusto fuera del flujo transaccional con timeouts
 *     finitos de conexion y lectura (2000 ms) y verificacion real del JSON de
 *     salud.
 *   - [Actividad 11 - P23/P25/P26]: Detecta nodos UP con su latencia real (P23),
 *     identifica caidas o respuestas degradadas como DOWN con latencia null (P25)
 *     y confirma la vuelta a UP al restablecer la instancia (P26).
 * ==============================================================================
 */
// Declara el paquete de clientes tecnicos de comunicacion externa del monitor.
package ec.edu.uta.monitor_balancer_service.client;

// Importa URI para construir de forma valida la direccion destino de cada sondeo.
import java.net.URI;
// Importa el cliente HTTP estandar de Java 11+ (java.net.http.HttpClient).
import java.net.http.HttpClient;
// Importa el constructor inmutable de solicitudes HTTP.
import java.net.http.HttpRequest;
// Importa el contenedor de respuestas HTTP con codigo de estado y cuerpo.
import java.net.http.HttpResponse;
// Importa Duration para expresar los tiempos limite en milisegundos.
import java.time.Duration;

// Importa @Value para inyectar los timeouts desde application.properties.
import org.springframework.beans.factory.annotation.Value;
// Importa @Component para registrar el cliente HTTP como bean reutilizable en Spring.
import org.springframework.stereotype.Component;

/**
 * Componente tecnico para las peticiones HTTP de sondeo de salud.
 * Configura timeouts finitos de conexion y lectura para evitar bloqueos indefinidos.
 * CORREGIDO (APE 04): Valida el contenido de la respuesta de Actuator exigiendo
 * explicitamente status "UP" en el cuerpo JSON, evitando interpretar cualquier 2xx como saludable.
 */
// Registra HealthCheckClient como componente singleton inyectable en MonitorService.
@Component
public class HealthCheckClient {

    // Instancia reutilizable y segura para hilos de java.net.http.HttpClient con timeout de conexion finito.
    private final HttpClient httpClient;
    // Duracion maxima permitida para recibir la respuesta completa de cada solicitud GET.
    private final Duration readTimeout;

    /**
     * Construye el cliente HTTP parametrizando los timeouts de conexion y lectura.
     *
     * @param connectTimeoutMs tiempo limite de conexion TCP en milisegundos (por defecto 2000 ms).
     * @param readTimeoutMs    tiempo limite de lectura de respuesta en milisegundos (por defecto 2000 ms).
     */
    public HealthCheckClient(
            // [Actividad 3] Inyecta monitor.client.connect-timeout-ms (2000 ms por defecto).
            @Value("${monitor.client.connect-timeout-ms:2000}") long connectTimeoutMs,
            // [Actividad 3] Inyecta monitor.client.read-timeout-ms (2000 ms por defecto).
            @Value("${monitor.client.read-timeout-ms:2000}") long readTimeoutMs) {
        // Convierte el timeout de lectura en objeto Duration reutilizado en cada HttpRequest.
        this.readTimeout = Duration.ofMillis(readTimeoutMs);
        // Crea el cliente HTTP configurando el limite finito de establecimiento de conexion TCP.
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    /**
     * Realiza una consulta GET sincrona con timeout estricto y validacion de Actuator.
     * [Actividad 3 y Actividad 11 - P23/P25/P26]: Evalua codigo HTTP y cuerpo JSON {"status":"UP"}.
     *
     * @param url direccion completa del endpoint de salud (/nginx-ping o /actuator/health).
     * @return HealthResult con estado (UP, DOWN o ERROR_ACCESO), latencia en ms, codigo HTTP y detalle.
     */
    public HealthResult check(String url) {
        // Registra el instante de inicio en milisegundos para medir la latencia real del sondeo.
        long start = System.currentTimeMillis();
        // Captura cualquier fallo de red, DNS, conexion rechazada o timeout para clasificar el nodo como DOWN.
        try {
            // Construye la peticion HTTP GET con la URI indicada y el timeout finito de lectura.
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(this.readTimeout)
                    .GET()
                    .build();

            // Ejecuta la llamada HTTP sincrona y obtiene el cuerpo de respuesta como cadena de texto.
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            // Calcula la latencia transcurrida garantizando un valor minimo de 1 ms para respuestas instantaneas locales.
            long latency = Math.max(1, System.currentTimeMillis() - start);
            // Extrae el codigo de estado HTTP retornado por el destino (200, 401, 503, etc.).
            int statusCode = response.statusCode();
            // Extrae el cuerpo textual JSON de la respuesta para validar el campo status.
            String body = response.body();

            // [Actividad 3 y Actividad 11 - P23] Validacion de Actuator y /nginx-ping: el cuerpo JSON debe contener explicitamente "status":"UP" (con o sin espacio y sin sensibilidad a mayusculas).
            boolean actuatorUp = body != null && (
                    body.contains("\"status\":\"UP\"") ||
                    body.contains("\"status\": \"UP\"") ||
                    body.toUpperCase().contains("\"STATUS\":\"UP\"") ||
                    body.toUpperCase().contains("\"STATUS\": \"UP\"")
            );

            // Evalua primero si la respuesta pertenece al rango HTTP exitoso 2xx.
            if (statusCode >= 200 && statusCode < 300) {
                // Solo considera UP al destino si ademas su JSON declara explicitamente status UP.
                if (actuatorUp) {
                    // Retorna resultado saludable UP con latencia medida y codigo HTTP 2xx.
                    return new HealthResult("UP", latency, statusCode, null);
                } else {
                    // Si responde 2xx pero sin {"status":"UP"} (ej. OUT_OF_SERVICE o pagina inesperada), marca DOWN.
                    return new HealthResult("DOWN", latency, statusCode,
                            "Respuesta HTTP " + statusCode + " pero Actuator no reporta status UP");
                }
            // Si el endpoint responde 401 o 403, clasifica el fallo de forma diferenciada como ERROR_ACCESO.
            } else if (statusCode == 401 || statusCode == 403) {
                // Reporta error de autenticacion/autorizacion en el endpoint de salud sondeado.
                return new HealthResult("ERROR_ACCESO", latency, statusCode, "Error de autorizacion HTTP " + statusCode);
            } else {
                // Cualquier otro codigo (por ejemplo 503 Service Unavailable o 500) clasifica el nodo como DOWN.
                return new HealthResult("DOWN", latency, statusCode, "Respuesta HTTP inesperada " + statusCode);
            }
        } catch (Exception ex) {
            // [Actividad 11 - P25] Ante SocketTimeoutException, ConnectException o puerto cerrado, clasifica DOWN con latencia y status null.
            return new HealthResult("DOWN", null, null, ex.getMessage());
        }
    }

    /**
     * Modelo inmutable interno que encapsula el resultado de un sondeo de salud individual.
     */
    public static class HealthResult {
        // Estado resultante del sondeo: UP, DOWN o ERROR_ACCESO.
        private final String estado; // UP, DOWN, ERROR_ACCESO
        // Latencia medida en milisegundos, o null cuando la conexion fallo por excepcion/timeout.
        private final Long latenciaMs;
        // Codigo HTTP retornado por el destino, o null cuando no hubo respuesta HTTP.
        private final Integer httpStatus;
        // Mensaje descriptivo de diagnostico en caso de anomalia, o null cuando el estado es UP.
        private final String mensaje;

        /**
         * Constructor completo del resultado de sondeo.
         *
         * @param estado     clasificacion de salud (UP, DOWN o ERROR_ACCESO).
         * @param latenciaMs tiempo de respuesta en ms (puede ser null si fallo la conexion).
         * @param httpStatus codigo HTTP recibido (puede ser null si no hubo respuesta).
         * @param mensaje    detalle tecnico en caso de error o null si esta UP.
         */
        public HealthResult(String estado, Long latenciaMs, Integer httpStatus, String mensaje) {
            // Asigna la clasificacion de estado.
            this.estado = estado;
            // Asigna la latencia medida.
            this.latenciaMs = latenciaMs;
            // Asigna el codigo HTTP obtenido.
            this.httpStatus = httpStatus;
            // Asigna el mensaje de diagnostico.
            this.mensaje = mensaje;
        }

        /**
         * Obtiene el estado de salud evaluado (UP, DOWN o ERROR_ACCESO).
         *
         * @return estado del destino.
         */
        public String getEstado() {
            return estado;
        }

        /**
         * Obtiene la latencia del sondeo en milisegundos.
         *
         * @return latencia en ms o null si el nodo no respondio.
         */
        public Long getLatenciaMs() {
            return latenciaMs;
        }

        /**
         * Obtiene el codigo de estado HTTP recibido del destino.
         *
         * @return codigo HTTP o null ante fallo de red.
         */
        public Integer getHttpStatus() {
            return httpStatus;
        }

        /**
         * Obtiene el mensaje explicativo de error cuando el destino no esta UP.
         *
         * @return mensaje diagnostico o null si el sondeo fue exitoso.
         */
        public String getMensaje() {
            return mensaje;
        }
    }
}
