package ec.edu.uta.venta_vehiculos_api.client;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.*;
import ec.edu.uta.venta_vehiculos_api.dto.NotificacionRequest;

// Cliente REST que envía notificaciones al microservicio externo
@Component
public class NotificacionClient {

    // Cliente HTTP de Spring para llamadas REST síncronas configuradas
    private final RestClient client;

    // Destinatario por defecto configurado para las notificaciones
    private final String destino;

    // Constructor que configura timeouts y encabezados de autenticación entre servicios
    public NotificacionClient(
            @Value("${servicios.notificaciones.url}") String url,
            @Value("${servicios.notificaciones.key}") String key,
            @Value("${servicios.notificaciones.destinatario}") String destino) {

        // Configura tiempos límite de conexión (2s) y lectura (3s) para evitar bloqueos
        var f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(2));
        f.setReadTimeout(Duration.ofSeconds(3));

        // Construye el cliente con la URL base, fábrica de peticiones y cabecera de seguridad
        client = RestClient.builder()
                .baseUrl(url)
                .requestFactory(f)
                .defaultHeader("X-Notification-Key", key)
                .build();
        this.destino = destino;
    }

    // Envía la notificación HTTP POST al endpoint /api/notificaciones
    public void enviar(Integer id, String tipo, String mensaje) {
        try {
            // Realiza el POST con el DTO NotificacionRequest y espera respuesta sin cuerpo
            client.post()
                    .uri("/api/notificaciones")
                    .body(new NotificacionRequest(id, destino, tipo, mensaje))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // En caso de que el servicio de notificaciones no esté disponible o falle, no rompe la transacción
            System.err.println("Notificación no enviada para vehículo " + id);
        }
    }
}