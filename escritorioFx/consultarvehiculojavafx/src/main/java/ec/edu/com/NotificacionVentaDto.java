/*
 * ============================================================================
 * Archivo: NotificacionVentaDto.java
 * Proyecto: Cliente de Escritorio JavaFX (consultarvehiculojavafx) - SOA APE 04
 * Función:
 *   Define el contrato DTO inmutable (Java Record) que representa cada registro
 *   de notificación de venta recibido en JSON desde GET /api/notificaciones vía
 *   NGINX (:8090), sin acceso directo del cliente a RabbitMQ ni a MySQL.
 * Actividades APE 04 cubiertas:
 *   - [Actividad 1 / Actividad 9]: Contrato DTO tipado con Jackson y soporte de
 *     alias @JsonAlias({"fechaRegistro", "fechaUTC"}) para garantizar
 *     interoperabilidad entre versiones del esquema de fecha del microservicio.
 *   - [Actividad 9]: Estructura de datos renderizada en la pestaña
 *     "Notificaciones en Vivo (RabbitMQ)" del cliente JavaFX.
 *   - [Actividad 10 - P27]: Exposición de ventaId, vehiculoId, compradorId y
 *     operacionId generados al ejecutar ventas reales desde la PWA.
 * ============================================================================
 */
// Declara el paquete raíz del módulo cliente JavaFX.
package ec.edu.com;

// Importa la anotación @JsonAlias de Jackson para aceptar múltiples nombres de campo JSON en la deserialización [Actividad 1 / Actividad 9].
import com.fasterxml.jackson.annotation.JsonAlias;
// Importa @JsonIgnoreProperties para tolerar campos adicionales futuros en el JSON sin romper el cliente [Actividad 1 / Actividad 9].
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Record que representa una notificación de venta recibida a través de RabbitMQ.
 */
// Indica a Jackson que ignore propiedades desconocidas presentes en la respuesta JSON del microservicio [Actividad 1 / Actividad 9].
@JsonIgnoreProperties(ignoreUnknown = true)
// Declara el record inmutable NotificacionVentaDto que mapea cada evento recibido desde GET /api/notificaciones [Actividad 1 / Actividad 9].
public record NotificacionVentaDto(
    // Identificador autoincremental del registro de notificación almacenado en notificaciones-service.
    Long id,
    // Identificador único del evento asíncrono propagado mediante RabbitMQ.
    String eventoId,
    // Tipo o categoría del evento de negocio notificado (por ejemplo, registro de venta).
    String tipo,
    // Identificador de la venta creada desde la PWA y propagada asíncronamente [Actividad 10 - P27].
    Long ventaId,
    // Identificador del vehículo vendido cuyo estado cambia de DISPONIBLE a VENDIDO [Actividad 10 - P27].
    Long vehiculoId,
    // Identificador del comprador asociado a la transacción generada en la PWA [Actividad 10 - P27].
    Long compradorId,
    // Identificador único (UUID) de la operación distribuida para trazabilidad e idempotencia [Actividad 10 - P27].
    String operacionId,
    // Mensaje descriptivo legible sobre el resultado o contenido de la notificación.
    String mensaje,
    // Estado actual del procesamiento o entrega de la notificación.
    String estado,
    // Marca temporal del evento; @JsonAlias permite mapear tanto "fechaRegistro" como "fechaUTC" al campo "fecha" [Actividad 1 / Actividad 9].
    @JsonAlias({"fechaRegistro", "fechaUTC"}) String fecha
) {}
