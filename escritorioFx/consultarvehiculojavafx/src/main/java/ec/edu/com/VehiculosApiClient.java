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
 * Cliente HTTP encargado de comunicarse de forma asíncrona con el servicio web REST de vehículos.
 */
public final class VehiculosApiClient {
    
    // URL base de la API REST del microservicio de vehículos
    private static final String URL_API = "http://127.0.0.1:8080/api/vehiculos";

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

        // Construcción de la solicitud HTTP GET
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(URL_API))
                                .timeout(Duration.ofSeconds(15)) // Tiempo máximo de espera para la respuesta
                                .header("Authorization", "Bearer " + jwt) // Encabezado de autenticación Bearer con espacio
                                .GET() // Método HTTP GET
                                .build();

        // Envío asíncrono de la petición HTTP recibiendo el cuerpo como cadena de texto (String)
        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                .thenApply(respuesta -> {
                    // Obtención del código de estado HTTP retornado por la API
                    int codigo = respuesta.statusCode();

                    // Si el código no es 200 OK (ej: 401 Unauthorized, 403 Forbidden, 500 Error), retorna lista vacía
                    if (codigo != 200) {
                        return new ResultadoConsulta(codigo, List.of());
                    }

                    try {
                        // Deserialización del cuerpo JSON a una lista de objetos Vehiculo
                        List<Vehiculo> vehiculos = mapper.readValue(
                            respuesta.body(), 
                            new TypeReference<List<Vehiculo>>() {}
                        );
                        // Retorna el resultado exitoso con el código 200 y la lista de vehículos
                        return new ResultadoConsulta(codigo, vehiculos);
                    } catch (JsonProcessingException ex) {
                        // En caso de error en el formato JSON, lanza una excepción de completación
                        throw new CompletionException(ex);
                    }
                });
    }

    /**
     * Record inmutable que encapsula la respuesta del servicio web:
     * el código de estado HTTP recibido y la lista de vehículos deserializada.
     */
    public record ResultadoConsulta(int codigoHttp, List<Vehiculo> vehiculos) {}
}
