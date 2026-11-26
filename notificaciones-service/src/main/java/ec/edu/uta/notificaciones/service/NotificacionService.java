/**
 * ==============================================================================
 * ARCHIVO: NotificacionService.java
 * COMPONENTE: notificaciones-service (Capa de Logica de Negocio)
 * ROL: Gestiona el registro idempotente de eventos de venta provenientes de
 *      RabbitMQ, el procesamiento validado de avisos HTTP POST y la consulta
 *      segura ante concurrencia del historial de notificaciones en orden inverso.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 8 y Actividad 9]: Deduplicacion estricta en memoria mediante
 *     ConcurrentHashMap.newKeySet(), almacenamiento seguro para hilos con
 *     CopyOnWriteArrayList insertando en posicion 0 (mas recientes primero) y
 *     exposicion inmutable del historial.
 *   - [Actividad 10 - P27]: Conserva ventaId y operacionId en cada registro
 *     del historial para su verificacion de trazabilidad en JavaFX.
 *   - [Actividad 11 - P22]: Descarte auditable de eventos repetidos con el
 *     mismo eventoId, evitando duplicados en el historial y registrando traza WARN.
 * ==============================================================================
 */
// Declara el paquete de servicios de negocio del microservicio de notificaciones.
package ec.edu.uta.notificaciones.service;

// Importa el DTO de cada entrada almacenada en el historial de notificaciones.
import ec.edu.uta.notificaciones.dto.NotificacionHistorialDto;
// Importa el DTO de entrada para notificaciones manuales via HTTP POST.
import ec.edu.uta.notificaciones.dto.NotificacionRequest;
// Importa el DTO de respuesta estandar para avisos manuales.
import ec.edu.uta.notificaciones.dto.NotificacionResponse;
// Importa el DTO que representa el evento de venta deserializado desde RabbitMQ.
import ec.edu.uta.notificaciones.dto.NotificacionVentaEvento;
// Importa la interfaz Logger de SLF4J para auditoria de registros y descartes.
import org.slf4j.Logger;
// Importa la fabrica de loggers SLF4J.
import org.slf4j.LoggerFactory;
// Importa @Service para registrar la logica de negocio como bean singleton en Spring.
import org.springframework.stereotype.Service;

// Importa LocalDateTime para obtener la fecha y hora local del registro.
import java.time.LocalDateTime;
// Importa DateTimeFormatter para formatear la marca temporal en formato legible.
import java.time.format.DateTimeFormatter;
// Importa utilidades de colecciones (List, Set, Collections, UUID).
import java.util.*;
// Importa ConcurrentHashMap para construir el conjunto concurrente de deduplicacion por eventoId.
import java.util.concurrent.ConcurrentHashMap;
// Importa CopyOnWriteArrayList para lecturas concurrentes sin bloqueos al servir GET /api/notificaciones.
import java.util.concurrent.CopyOnWriteArrayList;
// Importa AtomicLong para generar identificadores secuenciales unicos sin condiciones de carrera.
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio central de gestion de notificaciones asincronas (AMQP) y directas (HTTP).
 * Todas sus estructuras de datos en memoria son seguras para acceso concurrente
 * entre los hilos del consumidor RabbitMQ y los hilos HTTP de Tomcat.
 */
// Registra esta clase como servicio singleton administrado por Spring.
@Service
public class NotificacionService {

    // Logger estatico para dejar evidencia de eventos registrados, duplicados descartados y avisos directos.
    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    // Catalogo inmutable de tipos validos aceptados en solicitudes directas HTTP POST.
    private static final Set<String> TIPOS_PERMITIDOS =
            Set.of("REGISTRO", "ACTUALIZACION", "ELIMINACION", "VENTA");

    // Contador atomico thread-safe para asignar un identificador incremental unico a cada notificacion en el historial.
    private final AtomicLong contadorId = new AtomicLong(1);

    // [Actividad 8 y Actividad 9] Lista concurrente CopyOnWriteArrayList para almacenar el historial procesado y permitir lecturas seguras desde JavaFX.
    private final List<NotificacionHistorialDto> historial = new CopyOnWriteArrayList<>();

    // [Actividad 8, Actividad 9 y Actividad 11 - P22] Conjunto concurrente respaldado por ConcurrentHashMap para deduplicacion estricta y atomica por eventoId (Idempotencia AMQP).
    private final Set<String> eventosProcesados = ConcurrentHashMap.newKeySet();

    /**
     * Registra un evento de venta proveniente de RabbitMQ asegurando deduplicacion por eventoId.
     * Retorna true si fue registrado o false si se trato de un evento duplicado ya procesado.
     * [Actividad 8 y Actividad 9]: Procesamiento concurrente del evento AMQP.
     * [Actividad 10 - P27]: Almacena ventaId y operacionId para trazabilidad en JavaFX.
     * [Actividad 11 - P22]: Descarta de forma auditable cualquier repeticion del mismo eventoId.
     *
     * @param evento DTO del evento de venta consumido desde la cola notificaciones.ventas.
     * @return true si el evento es nuevo y fue agregado al historial; false si es nulo o duplicado.
     */
    public boolean registrarEventoVenta(NotificacionVentaEvento evento) {
        // Rechaza eventos nulos o sin clave de idempotencia eventoId para evitar registros inconsistentes.
        if (evento == null || evento.getEventoId() == null) {
            // Registra advertencia de evento malformado o incompleto.
            log.warn("Evento de venta nulo o sin identificador de evento");
            // Indica al consumidor que no se registro el evento.
            return false;
        }

        // [Actividad 11 - P22] Control de duplicados obligatorio: Set.add() en ConcurrentHashMap es atomico y retorna false si eventoId ya existe.
        if (!eventosProcesados.add(evento.getEventoId())) {
            // Emite traza WARN auditable comprobando que el reenvio duplicado fue descartado sin contaminar el historial.
            log.warn("EVENTO DUPLICADO DESCARTADO: eventoId={}", evento.getEventoId());
            // Retorna false para que el listener registre el descarte sin duplicar la entrada.
            return false;
        }

        // Construye el mensaje legible de negocio resumiendo la venta, vehiculo y comprador involucrados.
        String mensaje = String.format("Venta %d completada exitosamente. Vehículo #%d asignado al comprador #%d.",
                evento.getVentaId(), evento.getVehiculoId(), evento.getCompradorId());

        // Obtiene y formatea la fecha/hora actual de registro para mostrarla en la interfaz JavaFX.
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // [Actividad 10 - P27] Crea el registro de historial preservando eventoId, ventaId, vehiculoId, compradorId y operacionId.
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

        // [Actividad 8 y Actividad 9] Inserta en el indice 0 (orden cronologico inverso) para que las notificaciones mas recientes aparezcan primero en JavaFX.
        historial.add(0, notif);

        // [Actividad 10 - P27] Deja traza auditable en INFO vinculando ID local, ventaId y operacionId.
        log.info("NUEVA NOTIFICACION DE VENTA REGISTRADA: ID={}, ventaId={}, operacionId={}",
                notif.getId(), evento.getVentaId(), evento.getOperacionId());

        // Confirma que el evento unico fue procesado y almacenado exitosamente.
        return true;
    }

    /**
     * Retorna el historial completo de notificaciones para el cliente JavaFX.
     * [Actividad 8, Actividad 9 y Actividad 10 - P27]: Devuelve una vista inmutable
     * del CopyOnWriteArrayList manteniendo el orden inverso (mas recientes primero).
     *
     * @return lista inmutable de NotificacionHistorialDto.
     */
    public List<NotificacionHistorialDto> obtenerHistorial() {
        // Envuelve el historial en una vista de solo lectura para impedir modificaciones externas.
        return Collections.unmodifiableList(historial);
    }

    /**
     * Procesamiento de notificaciones manuales o directas via HTTP POST.
     * Valida todos los campos obligatorios y agrega el aviso al inicio del historial.
     * [Actividad 8 y Actividad 9]: Soporta integracion directa autenticada por X-Notificacion-Key.
     *
     * @param request DTO con los datos de la notificacion directa.
     * @return NotificacionResponse confirmando el envio.
     * @throws IllegalArgumentException si algun campo requerido es nulo, vacio o invalido (manejado como HTTP 400 por ApiExceptionHandler).
     */
    public NotificacionResponse procesar(NotificacionRequest request) {

        // Valida que vehiculoId exista y sea un identificador numerico positivo.
        if (request.getVehiculoId() == null || request.getVehiculoId() <= 0) {
            // Lanza excepcion de validacion capturada por ApiExceptionHandler como HTTP 400.
            throw new IllegalArgumentException("vehiculoId debe estar presente y ser válido");
        }

        // Valida que el destinatario no sea nulo ni contenga solo espacios en blanco.
        if (request.getDestinatario() == null || request.getDestinatario().trim().isEmpty()) {
            // Lanza excepcion de validacion para rechazar la peticion malformada.
            throw new IllegalArgumentException("El destinatario no puede estar vacío");
        }

        // Valida que el texto del mensaje este presente y no este en blanco.
        if (request.getMensaje() == null || request.getMensaje().trim().isEmpty()) {
            // Lanza excepcion de validacion por mensaje vacio.
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        // Valida que el tipo de notificacion pertenezca al conjunto permitido (sin distinguir mayusculas/minusculas).
        if (request.getTipo() == null || !TIPOS_PERMITIDOS.contains(request.getTipo().toUpperCase())) {
            // Lanza excepcion si el tipo no es REGISTRO, ACTUALIZACION, ELIMINACION ni VENTA.
            throw new IllegalArgumentException("Tipo de notificación no válido");
        }

        // Captura y formatea la marca temporal actual del registro.
        String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Construye el registro de historial generando un UUID como eventoId y marcando estado "ENVIADA".
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
        // Inserta el aviso manual al inicio de la lista concurrente (orden inverso).
        historial.add(0, notif);

        // Registra en bitacora el procesamiento de la notificacion directa.
        log.info("Notificacion directa recibida: Tipo={}, Vehiculo={}", request.getTipo(), request.getVehiculoId());

        // Devuelve la respuesta estandarizada de exito para el controlador (HTTP 201 Created).
        return new NotificacionResponse(
                true,
                "ENVIADA",
                "Notificación procesada correctamente"
        );
    }
}
