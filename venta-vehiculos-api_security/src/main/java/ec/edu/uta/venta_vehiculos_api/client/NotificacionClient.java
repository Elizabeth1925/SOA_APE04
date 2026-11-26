/**
 * Archivo: NotificacionClient.java
 * Responsabilidad arquitectónica:
 *   Cliente HTTP REST síncrono basado en RestClient de Spring Framework encargado
 *   de comunicar eventos de ciclo de vida de vehículos (REGISTRO, ACTUALIZACION,
 *   ELIMINACION) al microservicio externo de notificaciones (:8081). Implementa
 *   timeouts acotados y tolerancia a fallos para no bloquear transacciones ni degradar
 *   la disponibilidad del servicio.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Complementa los contratos REST CRUD de vehículos notificando cambios.
 *   - [Actividad 2]: Inyecta URL, clave de integración y destinatario desde configuración
 *     externa sin valores quemados en código fuente.
 *   - [Actividad 3]: Mantiene la resiliencia de las instancias A (:8080) y B (:8082) detrás
 *     de NGINX evitando que una caída de notificaciones afecte las respuestas de inventario.
 *   - [Actividad 11]: Autentica la comunicación servicio-a-servicio mediante el encabezado
 *     privado X-Notification-Key.
 */

// Declara el paquete destinado a clientes HTTP de integración externa.
package ec.edu.uta.venta_vehiculos_api.client;

// Importa Duration para expresar tiempos máximos de espera de conexión y lectura en segundos.
import java.time.Duration;
// Importa @Value para inyectar parámetros externalizados desde application.properties o entorno.
import org.springframework.beans.factory.annotation.Value;
// Importa @Component para registrar el cliente como bean singleton administrado por Spring.
import org.springframework.stereotype.Component;
// Importa SimpleClientHttpRequestFactory para parametrizar timeouts a nivel de socket HTTP.
import org.springframework.http.client.SimpleClientHttpRequestFactory;
// Importa RestClient y RestClientException para realizar llamadas HTTP síncronas y capturar fallos.
import org.springframework.web.client.*;
// Importa el DTO que representa el cuerpo JSON de la notificación saliente.
import ec.edu.uta.venta_vehiculos_api.dto.NotificacionRequest;

// [Actividad 2] Registra el cliente REST como componente inyectable en VehiculoController.
@Component
// Clase cliente que encapsula la invocación HTTP al microservicio de notificaciones.
public class NotificacionClient {

    // Instancia inmutable de RestClient preconfigurada con URL base, timeouts y clave de seguridad.
    private final RestClient client;

    // Correo o destinatario predeterminado inyectado desde configuración externa.
    private final String destino;

    // [Actividad 2] [Actividad 11] Constructor con inyección de propiedades externas para URL, clave y destinatario.
    public NotificacionClient(
            // Inyecta la URL base del servicio de notificaciones (ej. http://127.0.0.1:8081).
            @Value("${servicios.notificaciones.url}") String url,
            // Inyecta la clave compartida de integración sin exponer secretos en el código.
            @Value("${servicios.notificaciones.key}") String key,
            // Inyecta el destinatario por defecto al cual se dirigirán las notificaciones.
            @Value("${servicios.notificaciones.destinatario}") String destino) {

        // [Actividad 3] Crea la fábrica de solicitudes HTTP para acotar la latencia ante fallos del servicio remoto.
        var f = new SimpleClientHttpRequestFactory();
        // Define un timeout máximo de 2 segundos para establecer la conexión TCP.
        f.setConnectTimeout(Duration.ofSeconds(2));
        // Define un timeout máximo de 3 segundos esperando la respuesta HTTP del servicio de notificaciones.
        f.setReadTimeout(Duration.ofSeconds(3));

        // [Actividad 11] Construye el RestClient con URL base, timeouts y cabecera obligatoria X-Notification-Key.
        client = RestClient.builder()
                // Asigna la dirección base del microservicio de notificaciones.
                .baseUrl(url)
                // Adjunta la fábrica con los timeouts configurados.
                .requestFactory(f)
                // Adjunta en cada solicitud saliente la clave secreta de autenticación servicio-a-servicio.
                .defaultHeader("X-Notification-Key", key)
                // Finaliza y devuelve la instancia inmutable de RestClient.
                .build();
        // Almacena el destinatario por defecto en el atributo de instancia.
        this.destino = destino;
    }

    // [Actividad 1] [Actividad 3] Envía un aviso POST /api/notificaciones con degradación elegante ante errores de red.
    public void enviar(Integer id, String tipo, String mensaje) {
        // Inicia bloque protegido para aislar fallos del microservicio externo respecto al flujo principal.
        try {
            // Construye y ejecuta la petición HTTP POST hacia el recurso /api/notificaciones.
            client.post()
                    // Indica la ruta relativa del endpoint receptor de notificaciones.
                    .uri("/api/notificaciones")
                    // Serializa a JSON el DTO con identificador del vehículo, destinatario, tipo de evento y mensaje.
                    .body(new NotificacionRequest(id, destino, tipo, mensaje))
                    // Ejecuta la llamada HTTP y prepara el procesamiento de la respuesta.
                    .retrieve()
                    // Descarta el cuerpo de respuesta verificando únicamente el estado HTTP exitoso.
                    .toBodilessEntity();
        // Captura errores de conectividad, timeouts o respuestas HTTP 4xx/5xx del servicio externo.
        } catch (RestClientException e) {
            // [Actividad 3] Registra el fallo en consola de error sin propagar excepciones para no revertir la operación CRUD.
            System.err.println("Notificación no enviada para vehículo " + id);
        }
    }
}
