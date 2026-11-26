/*
 * ============================================================================
 * Archivo: VehiculosApiClient.java
 * Proyecto: Cliente de Escritorio JavaFX (consultarvehiculojavafx) - SOA APE 04
 * Función:
 *   Cliente HTTP asíncrono basado en java.net.http.HttpClient que centraliza
 *   toda la comunicación del escritorio JavaFX exclusivamente a través del
 *   Gateway NGINX (puerto 8090), evitando cualquier acceso directo a RabbitMQ,
 *   MySQL o puertos internos de los microservicios.
 * Actividades APE 04 cubiertas:
 *   - [Actividad 9]: Consumo de GET /api/vehiculos con cabecera Authorization
 *     Bearer JWT y consumo periódico de GET /api/notificaciones sin acceso
 *     directo a RabbitMQ/MySQL; encapsulación en ResultadoNotificaciones para
 *     diferenciar 3 estados visuales (conectado con datos, pantalla vacía
 *     saludable HTTP 200 [], fallo de servicio HTTP 502/503/504).
 *   - [Actividad 10 - P27]: Deserialización de NotificacionVentaDto con
 *     ventaId, vehiculoId, compradorId y operacionId generados por la PWA.
 *   - [Actividad 11]: Preservación exacta del código HTTP real para manejo de
 *     401/403 en vehículos y diferenciación explícita de fallos 502/503/504
 *     ante la detención del servicio de notificaciones.
 * ============================================================================
 */
// Declara el paquete principal del cliente JavaFX.
package ec.edu.com;

// Importaciones de Jackson para procesar y deserializar JSON [Actividad 1 / Actividad 9].
// Excepción verificada lanzada cuando el cuerpo JSON recibido es inválido o incompatible.
import com.fasterxml.jackson.core.JsonProcessingException;
// Utilidad de Jackson para conservar tipos genéricos (List<Vehiculo>, List<NotificacionVentaDto>) al deserializar.
import com.fasterxml.jackson.core.type.TypeReference;
// Motor principal de Jackson encargado de convertir cadenas JSON en objetos Java.
import com.fasterxml.jackson.databind.ObjectMapper;

// Importaciones del cliente HTTP estándar de Java (java.net.http) [Actividad 9].
// Representa identificadores URI validados para construir las peticiones HTTP.
import java.net.URI;
// Cliente HTTP nativo de Java con soporte para peticiones asíncronas no bloqueantes.
import java.net.http.HttpClient;
// Constructor inmutable de solicitudes HTTP (método, cabeceras y tiempos de espera).
import java.net.http.HttpRequest;
// Representación de la respuesta HTTP recibida (código de estado y cuerpo).
import java.net.http.HttpResponse;
// Permite definir tiempos máximos de espera (timeouts) en segundos.
import java.time.Duration;
// Interfaz de colección ordenada utilizada para devolver listas de vehículos y notificaciones.
import java.util.List;

// Importaciones para manejo de operaciones asíncronas y excepciones.
// Futuro componible que permite ejecutar llamadas HTTP sin congelar el hilo de UI de JavaFX [Actividad 9].
import java.util.concurrent.CompletableFuture;
// Envoltorio de excepción no verificada usado dentro de lambdas de CompletableFuture.
import java.util.concurrent.CompletionException;

/**
 * Cliente HTTP encargado de comunicarse de forma asíncrona con el Gateway NGINX
 * para la consulta de vehículos y notificaciones de ventas en tiempo real (RabbitMQ).
 */
// Clase final que agrupa las llamadas REST hacia el Gateway NGINX (:8090) [Actividad 9].
public final class VehiculosApiClient {
    
    // URL del catálogo de vehículos a través del Gateway NGINX (Puerto 8090, balanceo 3:1) [Actividad 9].
    private static final String URL_API = "http://127.0.0.1:8090/api/vehiculos";

    // URL del servicio de notificaciones a través de NGINX (Puerto 8090 -> :8081), sin acceso directo a RabbitMQ ni MySQL [Actividad 9].
    private static final String URL_NOTIFICACIONES = "http://127.0.0.1:8090/api/notificaciones";

    // Cliente HTTP con tiempo de espera de conexión configurado a 5 segundos para detectar caídas de red oportunamente [Actividad 11].
    private final HttpClient clienteHttp = HttpClient.newBuilder()
                                            .connectTimeout(Duration.ofSeconds(5))
                                            .build();

    // Objeto de Jackson para transformar la respuesta en formato JSON a objetos Java [Actividad 1 / Actividad 9].
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Consulta los vehículos en la API REST de forma asíncrona enviando el token JWT.
     * 
     * @param jwt Token de autenticación en formato String
     * @return CompletableFuture con el resultado de la consulta (código HTTP y lista de vehículos)
     */
    // Ejecuta GET /api/vehiculos vía NGINX (:8090) enviando el JWT si fue provisto [Actividad 9 / Actividad 11].
    public CompletableFuture<ResultadoConsulta> consultarVehiculos(String jwt) {
        // Construye la petición GET hacia /api/vehiculos con un timeout de respuesta de 15 segundos.
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(URL_API))
                                .timeout(Duration.ofSeconds(15))
                                .GET();

        // Adjunta la cabecera Authorization solo si existe un JWT no vacío; omitirlo permite verificar el rechazo HTTP 401 real [Actividad 9 / Actividad 11].
        if (jwt != null && !jwt.isBlank()) {
            // Añade el token en formato estándar Bearer eliminando espacios sobrantes.
            builder.header("Authorization", "Bearer " + jwt.trim());
        }

        // Finaliza y construye el objeto inmutable HttpRequest listo para envío.
        HttpRequest solicitud = builder.build();

        // Envía la solicitud HTTP de manera asíncrona solicitando el cuerpo como texto plano String [Actividad 9].
        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                // Procesa la respuesta recibida en segundo plano cuando el servidor responde.
                .thenApply(respuesta -> {
                    // Extrae el código de estado HTTP real devuelto por NGINX/backend (200, 401, 403, 502, 504, etc.) [Actividad 11].
                    int codigo = respuesta.statusCode();

                    // Si el estado no es 200 OK, devuelve una lista vacía conservando el código HTTP para que la UI informe 401/403 o error [Actividad 11].
                    if (codigo != 200) {
                        return new ResultadoConsulta(codigo, List.of());
                    }

                    // Intenta deserializar el cuerpo JSON en una lista tipada de registros Vehiculo [Actividad 1 / Actividad 9].
                    try {
                        // Convierte el arreglo JSON recibido en List<Vehiculo> usando Jackson TypeReference.
                        List<Vehiculo> vehiculos = mapper.readValue(
                            respuesta.body(), 
                            new TypeReference<List<Vehiculo>>() {}
                        );
                        // Retorna el resultado exitoso con código 200 y el catálogo real obtenido [Actividad 9].
                        return new ResultadoConsulta(codigo, vehiculos);
                    } catch (JsonProcessingException ex) {
                        // Propaga cualquier fallo de parseo JSON como CompletionException para ser capturado en la interfaz.
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
    // Ejecuta GET /api/notificaciones vía NGINX (:8090) para el sondeo en vivo sin conexión directa a RabbitMQ/MySQL [Actividad 9 / Actividad 11].
    public CompletableFuture<ResultadoNotificaciones> consultarNotificaciones() {
        // Construye la solicitud GET con timeout acotado de 5 segundos y preferencia explícita por respuesta JSON [Actividad 9].
        HttpRequest solicitud = HttpRequest.newBuilder(URI.create(URL_NOTIFICACIONES))
                                .timeout(Duration.ofSeconds(5))
                                .header("Accept", "application/json")
                                .GET()
                                .build();

        // Despacha la consulta de forma asíncrona para mantener fluida la interfaz JavaFX [Actividad 9].
        return clienteHttp.sendAsync(solicitud, HttpResponse.BodyHandlers.ofString())
                // Transforma la respuesta HTTP en un registro ResultadoNotificaciones con estado y diagnóstico preciso.
                .thenApply(respuesta -> {
                    // Obtiene el código de estado HTTP real retornado por el Gateway NGINX [Actividad 11].
                    int codigo = respuesta.statusCode();
                    // Evalúa si ocurrió un fallo HTTP (distinto de 200 OK) para diferenciarlo de una pantalla vacía saludable [Actividad 9 / Actividad 11].
                    if (codigo != 200) {
                        // Asigna un mensaje técnico específico según el código de error del gateway o microservicio [Actividad 11].
                        String detalle = switch (codigo) {
                            // 502 Bad Gateway: evidencia que notificaciones-service (:8081) fue detenido o rechazó la conexión [Actividad 11].
                            case 502 -> "HTTP 502 Bad Gateway: notificaciones-service (:8081) está detenido o no responde.";
                            // 503 Service Unavailable: el servicio reporta indisponibilidad temporal [Actividad 11].
                            case 503 -> "HTTP 503 Service Unavailable: servicio de notificaciones no disponible.";
                            // 504 Gateway Timeout: NGINX superó el tiempo de espera esperando respuesta del servicio [Actividad 11].
                            case 504 -> "HTTP 504 Gateway Timeout: tiempo de espera agotado hacia notificaciones-service.";
                            // 404 Not Found: la ruta configurada /api/notificaciones no existe en el proxy NGINX.
                            case 404 -> "HTTP 404 Not Found: ruta /api/notificaciones no encontrada en NGINX.";
                            // Cualquier otro código HTTP no exitoso se reporta dinámicamente con su número real.
                            default -> "HTTP " + codigo + ": error devuelto por el servicio de notificaciones.";
                        };
                        // Devuelve estado de fallo con lista vacía y detalle de error para activar el indicador visual ROJO [Actividad 9 / Actividad 11].
                        return new ResultadoNotificaciones(codigo, List.of(), detalle);
                    }
                    // Si el estado fue HTTP 200, intenta deserializar la lista JSON de NotificacionVentaDto [Actividad 1 / Actividad 9 / Actividad 10 - P27].
                    try {
                        // Parsea el arreglo JSON (pudiendo ser [] o contener ventas reales de la PWA) a List<NotificacionVentaDto>.
                        List<NotificacionVentaDto> lista = mapper.readValue(
                            respuesta.body(),
                            new TypeReference<List<NotificacionVentaDto>>() {}
                        );
                        // Retorna HTTP 200 con errorDetalle=null: si lista está vacía activa estado AZUL saludable; si trae datos activa estado VERDE [Actividad 9].
                        return new ResultadoNotificaciones(200, lista, null);
                    } catch (JsonProcessingException ex) {
                        // Si el servidor devolvió HTTP 200 con contenido corrupto no-JSON, informa detalle de error para estado ROJO [Actividad 11].
                        return new ResultadoNotificaciones(200, List.of(), "Respuesta JSON inválida del servicio de notificaciones.");
                    }
                });
    }

    /**
     * Record inmutable que encapsula la respuesta del servicio de vehículos.
     */
    // DTO de respuesta interna que agrupa el código HTTP real y la lista de vehículos consultados [Actividad 9 / Actividad 11].
    public record ResultadoConsulta(int codigoHttp, List<Vehiculo> vehiculos) {}

    /**
     * Record inmutable que encapsula el resultado del sondeo de notificaciones.
     */
    // DTO de respuesta interna que permite distinguir en UI: VERDE (200 con datos), AZUL (200 vacío y errorDetalle null) y ROJO (502/504 o errorDetalle no nulo) [Actividad 9 / Actividad 11].
    public record ResultadoNotificaciones(int codigoHttp, List<NotificacionVentaDto> notificaciones, String errorDetalle) {}
}
