/**
 * ==============================================================================
 * ARCHIVO: VentasRabbitListener.java
 * COMPONENTE: notificaciones-service (Consumidor AMQP / RabbitMQ)
 * ROL: Escucha de forma asincrona los eventos JSON de venta publicados por
 *      ventas-api (FastAPI) en la cola durable de RabbitMQ y delega su registro
 *      idempotente al servicio de notificaciones.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Consumidor AMQP real mediante @RabbitListener
 *     suscrito a la cola durable "notificaciones.ventas".
 *   - [Actividad 10 - P27]: Recepcion y trazabilidad del evento con ventaId y
 *     operacionId para su visualizacion posterior en el cliente JavaFX.
 *   - [Actividad 11 - P22]: Confirmacion de procesamiento unico y registro
 *     auditable cuando un evento duplicado es descartado por eventoId.
 * ==============================================================================
 */
// Declara el paquete de consumidores de mensajeria asincrona del servicio.
package ec.edu.uta.notificaciones.consumer;

// Importa ObjectMapper de Jackson para deserializar el payload JSON recibido desde RabbitMQ.
import com.fasterxml.jackson.databind.ObjectMapper;
// Importa el DTO que representa el contrato JSON del evento de venta publicado por ventas-api.
import ec.edu.uta.notificaciones.dto.NotificacionVentaEvento;
// Importa el servicio de negocio encargado de deduplicar y almacenar el historial en memoria.
import ec.edu.uta.notificaciones.service.NotificacionService;
// Importa la interfaz Logger de SLF4J para emitir trazas estructuradas de auditoria.
import org.slf4j.Logger;
// Importa la fabrica de loggers SLF4J asociada a esta clase.
import org.slf4j.LoggerFactory;
// Importa la anotacion de Spring AMQP para suscribir un metodo a una cola de RabbitMQ.
import org.springframework.amqp.rabbit.annotation.RabbitListener;
// Importa la anotacion @Component para registrar automaticamente el consumidor como bean de Spring.
import org.springframework.stereotype.Component;

/**
 * Consumidor AMQP que recibe mensajes de la cola durable notificaciones.ventas.
 * Garantiza desacoplamiento total entre el cierre de la venta en FastAPI y el
 * registro del aviso en notificaciones-service.
 */
// Registra esta clase en el contenedor de Spring para que Spring AMQP active su escucha automaticamente.
@Component
public class VentasRabbitListener {

    // Logger estatico para registrar recepcion, procesamiento exitoso, descartes duplicados y errores JSON.
    private static final Logger log = LoggerFactory.getLogger(VentasRabbitListener.class);

    // Dependencia inyectada del servicio que aplica idempotencia y almacena el historial concurrente.
    private final NotificacionService notificacionService;
    // Dependencia inyectada de Jackson para convertir el texto JSON del mensaje en NotificacionVentaEvento.
    private final ObjectMapper objectMapper;

    /**
     * Constructor con inyeccion de dependencias administrado por Spring.
     *
     * @param notificacionService servicio de registro y deduplicacion de notificaciones.
     * @param objectMapper        serializador/deserializador JSON configurado por Spring Boot.
     */
    public VentasRabbitListener(NotificacionService notificacionService, ObjectMapper objectMapper) {
        // Asigna el servicio de notificaciones al atributo inmutable.
        this.notificacionService = notificacionService;
        // Asigna el ObjectMapper al atributo inmutable.
        this.objectMapper = objectMapper;
    }

    /**
     * Consumidor que escucha la cola durable notificaciones.ventas.
     * Si el mensaje fue recibido previamente (mismo eventoId), se descarta sin duplicar.
     * [Actividad 8 y Actividad 9]: Consumo real de eventos AMQP desde RabbitMQ.
     * [Actividad 10 - P27]: Procesa eventos con ventaId y operacionId visibles en JavaFX.
     * [Actividad 11 - P22]: Registra advertencia auditable si el eventoId ya fue procesado.
     *
     * @param mensajeJson cuerpo textual en formato JSON entregado por RabbitMQ.
     */
    // [Actividad 8 y Actividad 9] Suscribe este metodo a la cola configurada (por defecto "notificaciones.ventas").
    @RabbitListener(queues = "${app.rabbitmq.queue:notificaciones.ventas}")
    public void recibirMensajeVenta(String mensajeJson) {
        // Registra en bitacora el contenido crudo del mensaje recibido desde el broker AMQP.
        log.info("Mensaje recibido de RabbitMQ: {}", mensajeJson);
        // Bloque try-catch para evitar que un mensaje malformado provoque bucles infinitos de reentrega.
        try {
            // Deserializa la cadena JSON al objeto DTO NotificacionVentaEvento con sus campos de trazabilidad.
            NotificacionVentaEvento evento = objectMapper.readValue(mensajeJson, NotificacionVentaEvento.class);
            // [Actividad 11 - P22] Delega el registro aplicando deduplicacion estricta por eventoId en memoria.
            boolean procesado = notificacionService.registrarEventoVenta(evento);
            // Evalua si el evento era nuevo y quedo incorporado al historial consultable por JavaFX.
            if (procesado) {
                // [Actividad 10 - P27] Confirma en log el procesamiento exitoso asociando el ventaId.
                log.info("Evento de venta procesado y confirmado: ventaId={}", evento.getVentaId());
            } else {
                // [Actividad 11 - P22] Deja evidencia auditable de descarte por duplicidad o invalidez del eventoId.
                log.warn("Evento ya existente o descartado: eventoId={}", evento.getEventoId());
            }
        } catch (Exception ex) {
            // Registra el error de deserializacion o procesamiento sin tumbar el contenedor del listener AMQP.
            log.error("Error al procesar mensaje JSON de RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
