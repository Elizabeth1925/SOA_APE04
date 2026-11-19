package ec.edu.uta.notificaciones.service;

import ec.edu.uta.notificaciones.dto.NotificacionHistorialDto;
import ec.edu.uta.notificaciones.dto.NotificacionRequest;
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
import ec.edu.uta.notificaciones.dto.NotificacionVentaEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("REGISTRO", "ACTUALIZACION", "ELIMINACION", "VENTA");

    // Contador atomico para identificador incremental de notificaciones en historial
    private final AtomicLong contadorId = new AtomicLong(1);

    // Lista concurrente para almacenar el historial de notificaciones procesadas
    private final List<NotificacionHistorialDto> historial = new CopyOnWriteArrayList<>();

    // Conjunto concurrente para control estricto de duplicidad por eventoId (Idempotencia AMQP)
    private final Set<String> eventosProcesados = ConcurrentHashMap.newKeySet();

    /**
     * Registra un evento de venta proveniente de RabbitMQ asegurando deduplicacion por eventoId.
     * Retorna true si fue registrado o false si se trato de un evento duplicado ya procesado.
     */
    public boolean registrarEventoVenta(NotificacionVentaEvento evento) {
        if (evento == null || evento.getEventoId() == null) {
            log.warn("Evento de venta nulo o sin identificador de evento");
            return false;
        }

        // Control de duplicados obligatorio (Prueba P22 de la guia)
        if (!eventosProcesados.add(evento.getEventoId())) {
            log.warn("EVENTO DUPLICADO DESCARTADO: eventoId={}", evento.getEventoId());
            return false;
        }

        String mensaje = String.format("Venta %d completada exitosamente. Vehículo #%d asignado al comprador #%d.",
                evento.getVentaId(), evento.getVehiculoId(), evento.getCompradorId());

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        NotificacionHistorialDto notif = new NotificacionHistorialDto(
                contadorId.getAndIncrement(),
                evento.getEventoId(),
                "VENTA",
                evento.getVentaId(),
                evento.getVehiculoId(),
                evento.getCompradorId(),
                evento.getOperacionId(),
                mensaje,
                "PROCESADA",
                fecha
        );

        // Agrega al inicio para que las mas recientes aparezcan primero
        historial.add(0, notif);

        log.info("NUEVA NOTIFICACION DE VENTA REGISTRADA: ID={}, ventaId={}, operacionId={}",
                notif.getId(), evento.getVentaId(), evento.getOperacionId());

        return true;
    }

    /**
     * Retorna el historial completo de notificaciones para el cliente JavaFX.
     */
    public List<NotificacionHistorialDto> obtenerHistorial() {
        return Collections.unmodifiableList(historial);
    }

    /**
     * Procesamiento de notificaciones manuales o directas via HTTP POST.
     */
    public NotificacionResponse procesar(NotificacionRequest request) {

        if (request.getVehiculoId() == null || request.getVehiculoId() <= 0) {
            throw new IllegalArgumentException("vehiculoId debe estar presente y ser válido");
        }

        if (request.getDestinatario() == null || request.getDestinatario().trim().isEmpty()) {
            throw new IllegalArgumentException("El destinatario no puede estar vacío");
        }

        if (request.getMensaje() == null || request.getMensaje().trim().isEmpty()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if (request.getTipo() == null || !TIPOS_PERMITIDOS.contains(request.getTipo().toUpperCase())) {
            throw new IllegalArgumentException("Tipo de notificación no válido");
        }

        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        NotificacionHistorialDto notif = new NotificacionHistorialDto(
                contadorId.getAndIncrement(),
                UUID.randomUUID().toString(),
                request.getTipo().toUpperCase(),
                null,
                request.getVehiculoId(),
                null,
                null,
                request.getMensaje(),
                "ENVIADA",
                fecha
        );
        historial.add(0, notif);

        log.info("Notificacion directa recibida: Tipo={}, Vehiculo={}", request.getTipo(), request.getVehiculoId());

        return new NotificacionResponse(
                true,
                "ENVIADA",
                "Notificación procesada correctamente"
        );
    }
}
