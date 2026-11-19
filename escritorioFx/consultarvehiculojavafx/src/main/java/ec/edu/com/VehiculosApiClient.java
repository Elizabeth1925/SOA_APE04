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
     * Consulta las notificaciones de ventas recibidas vía RabbitMQ en Notificaciones-Service.
     * 
     * @return CompletableFuture con la lista de notificaciones deserializadas
     */
    public CompletableFuture<List<NotificacionVentaDto>> consultarNotificaciones() {
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(URL_NOTIFICACIONES))
                                .timeout(Duration.ofSeconds(8))
                                .header("Accept", "application/json")
                                .GET()
                                .build();

        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenApply(respuesta -> {
                    if (respuesta.statusCode() != 200) {
                        return List.<NotificacionVentaDto>of();
                    }
                    try {
                        return mapper.readValue(
                            respuesta.body(),
                            new TypeReference<List<NotificacionVentaDto>>() {}
                        );
                    } catch (JsonProcessingException ex) {
                        return List.<NotificacionVentaDto>of();
                    }
                });
    }

    /**
     * Record inmutable que encapsula la respuesta del servicio web:
     * el código de estado HTTP recibido y la lista de vehículos deserializada.
     */
    public record ResultadoConsulta(int codigoHttp, List<Vehiculo> vehiculos) {}
}
