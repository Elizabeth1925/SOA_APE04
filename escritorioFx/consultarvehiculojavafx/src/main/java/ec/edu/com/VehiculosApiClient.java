package ec.edu.com;

// Importaciones de Jackson para procesar y deserializar JSON
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

// Importaciones del cliente HTTP estándar de Java (java.net.http)
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

// Importaciones para manejo de operaciones asíncronas y excepciones
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Cliente HTTP encargado de comunicarse de forma asíncrona con el Gateway NGINX
 * para la consulta de vehículos y notificaciones de ventas en tiempo real (RabbitMQ).
 */
public final class VehiculosApiClient {
    
    // URL del catálogo de vehículos a través del Gateway NGINX (Puerto 8090, balanceo 3:1)
    private static final String URL_API = "http://127.0.0.1:8090/api/vehiculos";

    // URL del servicio de notificaciones a través de NGINX (Puerto 8090 / 8081)
    private static final String URL_NOTIFICACIONES = "http://127.0.0.1:8090/api/notificaciones";

    // Cliente HTTP con tiempo de espera de conexión configurado a 5 segundos
    private final HttpClient clienteHttp = HttpClient.newBuilder()
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .build();

    // Objeto de Jackson para transformar la respuesta en formato JSON a objetos Java
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Consulta los vehículos en la API REST de forma asíncrona enviando el token JWT.
     * 
     * @param jwt Token de autenticación en formato String
     * @return CompletableFuture con el resultado de la consulta (código HTTP y lista de vehículos)
     */
    public CompletableFuture<ResultadoConsulta> consultarVehiculos(String jwt) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(URL_API))
                                .timeout(Duration.ofSeconds(15))
                                .GET();

        if (jwt != null && !jwt.isBlank()) {
            builder.header("Authorization", "Bearer " + jwt.trim());
        }

        HttpRequest solicitud = builder.build();

        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenApply(respuesta -> {
                    int codigo = respuesta.statusCode();

                    if (codigo != 200) {
                        return new ResultadoConsulta(codigo, List.of());
                    }

                    try {
                        List<Vehiculo> vehiculos = mapper.readValue(
                            respuesta.body(), 
                            new TypeReference<List<Vehiculo>>() {}
                        );
                        return new ResultadoConsulta(codigo, vehiculos);
                    } catch (JsonProcessingException ex) {
                        throw new CompletionException(ex);
                    }
                });
    }

    /**
     * Consulta las notificaciones de ventas recibidas vía RabbitMQ en Notificaciones-Service,
     * preservando el código HTTP real para distinguir pantalla vacía (200 con lista vacía)
     * de un fallo de servicio (502/503/504 o error de red).
     *
     * @return CompletableFuture con código HTTP, lista de notificaciones y detalle de error si aplica
     */
    public CompletableFuture<ResultadoNotificaciones> consultarNotificaciones() {
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(URL_NOTIFICACIONES))
                                .timeout(Duration.ofSeconds(5))
                                .header("Accept", "application/json")
                                .GET()
                                .build();

        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenApply(respuesta -> {
                    int codigo = respuesta.statusCode();
                    if (codigo != 200) {
                        String detalle = switch (codigo) {
                            case 502 -> "HTTP 502 Bad Gateway: notificaciones-service (:8081) está detenido o no responde.";
                            case 503 -> "HTTP 503 Service Unavailable: servicio de notificaciones no disponible.";
                            case 504 -> "HTTP 504 Gateway Timeout: tiempo de espera agotado hacia notificaciones-service.";
                            case 404 -> "HTTP 404 Not Found: ruta /api/notificaciones no encontrada en NGINX.";
                            default -> "HTTP " + codigo + ": error devuelto por el servicio de notificaciones.";
                        };
                        return new ResultadoNotificaciones(codigo, List.of(), detalle);
                    }
                    try {
                        List<NotificacionVentaDto> lista = mapper.readValue(
                            respuesta.body(),
                            new TypeReference<List<NotificacionVentaDto>>() {}
                        );
                        return new ResultadoNotificaciones(200, lista, null);
                    } catch (JsonProcessingException ex) {
                        return new ResultadoNotificaciones(200, List.of(), "Respuesta JSON inválida del servicio de notificaciones.");
                    }
                });
    }

    /**
     * Record inmutable que encapsula la respuesta del servicio de vehículos.
     */
    public record ResultadoConsulta(int codigoHttp, List<Vehiculo> vehiculos) {}

    /**
     * Record inmutable que encapsula el resultado del sondeo de notificaciones.
     */
    public record ResultadoNotificaciones(int codigoHttp, List<NotificacionVentaDto> notificaciones, String errorDetalle) {}
}
