package ec.edu.uta.notificaciones.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.edu.uta.notificaciones.dto.NotificacionVentaEvento;
import ec.edu.uta.notificaciones.service.NotificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class VentasRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(VentasRabbitListener.class);

    private final NotificacionService notificacionService;
    private final ObjectMapper objectMapper;

    public VentasRabbitListener(NotificacionService notificacionService, ObjectMapper objectMapper) {
        this.notificacionService = notificacionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Consumidor que escucha la cola durable notificaciones.ventas.
     * Si el mensaje fue recibido previamente (mismo eventoId), se descarta sin duplicar.
     */
    @RabbitListener(queues = "${app.rabbitmq.queue:notificaciones.ventas}")
    public void recibirMensajeVenta(String mensajeJson) {
        log.info("Mensaje recibido de RabbitMQ: {}", mensajeJson);
        try {
            NotificacionVentaEvento evento = objectMapper.readValue(mensajeJson, NotificacionVentaEvento.class);
            boolean procesado = notificacionService.registrarEventoVenta(evento);
            if (procesado) {
                log.info("Evento de venta procesado y confirmado: ventaId={}", evento.getVentaId());
            } else {
                log.warn("Evento ya existente o descartado: eventoId={}", evento.getEventoId());
            }
        } catch (Exception ex) {
            log.error("Error al procesar mensaje JSON de RabbitMQ: {}", ex.getMessage(), ex);
        }
    }
}
