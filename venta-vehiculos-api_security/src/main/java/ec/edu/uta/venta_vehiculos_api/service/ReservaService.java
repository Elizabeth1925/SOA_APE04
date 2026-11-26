/**
 * Archivo: ReservaService.java
 * Responsabilidad arquitectónica:
 *   Capa de servicio transaccional que implementa la máquina de estados y control de
 *   concurrencia de las reservas de vehículos en coordinación con el orquestador
 *   ventas-api (FastAPI). Ejecuta transacciones con aislamiento READ_COMMITTED,
 *   bloqueo pesimista nativo SQL FOR UPDATE compatible con MariaDB 10.4, idempotencia
 *   por operacionId y refresco explícito del contexto de persistencia JPA.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Implementa el protocolo de orquestación en 2 fases con ventas-api:
 *       * Fase 1 (reservar): adquiere bloqueo pesimista FOR UPDATE, garantiza idempotencia
 *         por operacionId y transita el vehículo DISPONIBLE -> RESERVADO (reserva RESERVADA).
 *       * Fase 2 commit (confirmar): bloquea el vehículo, refresca estado y transita
 *         RESERVADA -> CONFIRMADA y vehículo RESERVADO -> VENDIDO.
 *       * Fase 2 compensación (liberar): bloquea el vehículo, refresca estado y revierte
 *         RESERVADA -> LIBERADA y vehículo RESERVADO -> DISPONIBLE.
 *   - [Actividad 10 - P27]: Mediante aislamiento READ_COMMITTED + bloqueo pesimista
 *     FOR UPDATE sobre MariaDB compartida + EntityManager.refresh(), garantiza coherencia
 *     transversal de inventario y reservas entre Instancia A (:8080) e Instancia B (:8082)
 *     sin carreras ni estados obsoletos en caché de primer nivel.
 */

// Declara el paquete que concentra los servicios de lógica de negocio.
package ec.edu.uta.venta_vehiculos_api.service;

// Importa UUID para generar identificadores únicos globales de reserva.
import java.util.UUID;
// Importa @Service para registrar la clase como bean de servicio Spring.
import org.springframework.stereotype.Service;
// Importa @Transactional e Isolation para controlar los límites y aislamiento transaccional.
import org.springframework.transaction.annotation.*;
// Importa el DTO de respuesta ReservaResponse.
import ec.edu.uta.venta_vehiculos_api.dto.*;
// Importa la excepción de dominio controlada ApiError (404 / 409).
import ec.edu.uta.venta_vehiculos_api.exception.ApiError;
// Importa las entidades JPA Reserva y Vehiculo.
import ec.edu.uta.venta_vehiculos_api.model.*;
// Importa los repositorios JPA VehiculoRepository y ReservaRepository.
import ec.edu.uta.venta_vehiculos_api.repository.*;

// [Actividad 4] [Actividad 10 - P27] Declara el servicio de gestión transaccional de reservas.
@Service
// [Actividad 4] [Actividad 10 - P27] Aplica aislamiento READ_COMMITTED para leer siempre cambios confirmados por otra instancia (A o B) tras esperar un bloqueo.
@Transactional(isolation=Isolation.READ_COMMITTED)
// Servicio encargado de reservar, consultar, confirmar y liberar vehículos en 2 fases.
public class ReservaService {

    // Repositorio de vehículos que provee el bloqueo pesimista nativo SQL FOR UPDATE.
    private final VehiculoRepository vehiculos;
    // Repositorio de reservas que persiste y consulta por UUID y por operacionId.
    private final ReservaRepository reservas;

    // Constructor con inyección de dependencias de ambos repositorios JPA.
    public ReservaService(VehiculoRepository v, ReservaRepository r) {
        // Asigna el repositorio de vehículos.
        vehiculos = v;
        // Asigna el repositorio de reservas.
        reservas = r;
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 1 de orquestación: crea una reserva idempotente y cambia el vehículo de DISPONIBLE a RESERVADO.
    public ReservaResponse reservar(int id, String op) {
        // [Actividad 4] [Actividad 10 - P27] Adquiere bloqueo pesimista nativo FOR UPDATE (MariaDB 10.4) sobre la fila del vehículo; lanza 404 si no existe.
        Vehiculo v = vehiculos.bloquear(id).orElseThrow(() ->
            new ApiError(404, "VEHICULO_NO_ENCONTRADO", "Vehículo inexistente"));

        // [Actividad 4] [Actividad 10 - P27] Verificación de idempotencia: busca si esta operacionId ya fue procesada previamente (incluso en la otra instancia).
        var anterior = reservas.findByOperacionId(op);
        // Si ya existe una reserva con la misma clave de operación, evita crear duplicados.
        if (anterior.isPresent()) {
            // Recupera la reserva existente asociada a operacionId.
            Reserva r = anterior.get();
            // Comprueba que no se esté intentando reutilizar la misma operacionId para un vehículo distinto.
            if (!r.vehiculo.getIdVehiculo().equals(id))
                // Lanza HTTP 409 Conflict si existe discrepancia de vehículo para la misma operación.
                throw conflicto("La operación pertenece a otro vehículo");

            // Idempotencia exitosa: retorna el estado de la reserva ya existente sin alterar el inventario.
            return ReservaResponse.de(r);
        }

        // [Actividad 4] Regla de negocio: solo un vehículo en estado DISPONIBLE puede pasar a RESERVADO.
        if (!"DISPONIBLE".equals(v.getEstado()))
            // Rechaza con HTTP 409 Conflict si el vehículo ya está RESERVADO o VENDIDO.
            throw conflicto("El vehículo no está disponible");

        // Instancia la nueva entidad Reserva congelando el precio vigente del vehículo.
        Reserva r = new Reserva();
        // Genera el identificador UUID único de la reserva.
        r.id = UUID.randomUUID().toString();
        // Asigna la clave de idempotencia recibida desde ventas-api.
        r.operacionId = op;
        // Vincula la reserva con la entidad Vehiculo bloqueada.
        r.vehiculo = v;
        // Congela el precio actual del vehículo en el registro de la reserva.
        r.precio = v.getPrecio();
        // Define la moneda estándar de la operación en dólares estadounidenses ("USD").
        r.moneda = "USD";
        // [Actividad 4] Establece el estado inicial de la reserva como RESERVADA.
        r.estado = "RESERVADA";

        // [Actividad 4] Ejecuta la transición de estado del vehículo: DISPONIBLE -> RESERVADO.
        v.setEstado("RESERVADO");
        // Persiste la reserva y fuerza flush inmediato para detectar colisiones de unicidad antes del commit.
        reservas.saveAndFlush(r);

        // Devuelve el DTO con los detalles de la reserva recién creada.
        return ReservaResponse.de(r);
    }

    // [Actividad 4] [Actividad 10 - P27] Consulta en modo solo lectura una reserva por su identificador UUID.
    @Transactional(readOnly=true)
    public ReservaResponse obtener(String id) {
        // Busca la reserva (o lanza 404) y la transforma al DTO ReservaResponse.
        return ReservaResponse.de(buscar(id));
    }

    // [Actividad 4] [Actividad 10 - P27] Consulta en modo solo lectura una reserva a partir de su operacionId de idempotencia.
    @Transactional(readOnly=true)
    public ReservaResponse porOperacion(String op) {
        // Busca por operacionId en la base compartida; si no existe, lanza ApiError 404.
        return ReservaResponse.de(reservas.findByOperacionId(op)
            .orElseThrow(() -> new ApiError(404, "RESERVA_NO_ENCONTRADA",
                "La operación no tiene reserva")));
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 2 (commit): confirma la reserva (RESERVADA -> CONFIRMADA) y marca el vehículo como VENDIDO.
    public ReservaResponse confirmar(String id) {
        // Adquiere bloqueo pesimista FOR UPDATE sobre el vehículo asociado y refresca ambas entidades desde BD.
        Reserva r = bloquear(id);

        // Impide confirmar una reserva que ya fue compensada/liberada anteriormente.
        if ("LIBERADA".equals(r.estado))
            // Lanza HTTP 409 Conflict indicando que la reserva ya fue liberada.
            throw conflicto("La reserva fue liberada");

        // Si la reserva sigue en estado RESERVADA procede con la transición (si ya era CONFIRMADA, resulta idempotente).
        if ("RESERVADA".equals(r.estado)) {
            // Verifica coherencia estricta: el vehículo vinculado debe encontrarse en estado RESERVADO.
            if (!"RESERVADO".equals(r.vehiculo.getEstado()))
                // Lanza HTTP 409 Conflict si se detecta un estado anómalo en el vehículo.
                throw conflicto("Estado del vehículo inconsistente");

            // [Actividad 4] Transita la reserva a CONFIRMADA y completa el ciclo del vehículo: RESERVADO -> VENDIDO.
            r.estado = "CONFIRMADA";
            // Marca definitivamente el vehículo como VENDIDO (Hibernate sincroniza al hacer commit).
            r.vehiculo.setEstado("VENDIDO");
        }

        // Retorna el DTO actualizado reflejando la reserva confirmada.
        return ReservaResponse.de(r);
    }

    // [Actividad 4] [Actividad 10 - P27] Fase 2 (compensación): libera la reserva (RESERVADA -> LIBERADA) y devuelve el vehículo a DISPONIBLE.
    public void liberar(String id) {
        // Adquiere bloqueo pesimista FOR UPDATE sobre el vehículo y refresca estado desde MariaDB.
        Reserva r = bloquear(id);

        // Protege ventas definitivas: no es válido liberar una reserva que ya fue confirmada.
        if ("CONFIRMADA".equals(r.estado))
            // Lanza HTTP 409 Conflict bloqueando la reversión de una venta confirmada.
            throw conflicto("No se libera una reserva confirmada");

        // Si la reserva estaba activa (RESERVADA), aplica la compensación (si ya estaba LIBERADA, es idempotente).
        if ("RESERVADA".equals(r.estado)) {
            // [Actividad 4] Marca la reserva como LIBERADA.
            r.estado = "LIBERADA";
            // [Actividad 4] Revierte el estado del vehículo a DISPONIBLE para que pueda volver a venderse.
            r.vehiculo.setEstado("DISPONIBLE");
        }
    }

    // [Actividad 4] [Actividad 10 - P27] Serializa operaciones sobre una reserva bloqueando su vehículo con FOR UPDATE y refrescando caché JPA.
    private Reserva bloquear(String id) {
        // Primero localiza la reserva por UUID (lanza 404 si no existe).
        Reserva r = buscar(id);
        // Bloquea pesimistamente la fila del vehículo en MariaDB para serializar cualquier acceso concurrente de Instancia A o B.
        vehiculos.bloquear(r.vehiculo.getIdVehiculo()).orElseThrow();
        // Refresca la reserva y el vehículo desde BD después de adquirir el bloqueo para descartar lecturas previas a la espera.
        return refrescar(r);
    }

    // Inyecta el EntityManager de la transacción activa para ejecutar refrescos directos desde la base de datos.
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    // [Actividad 10 - P27] Recarga desde MariaDB el estado real confirmado de Reserva y Vehiculo tras adquirir el bloqueo FOR UPDATE.
    private Reserva refrescar(Reserva r) {
        // Descarta el estado cacheado de la reserva y lee la fila confirmada más reciente.
        em.refresh(r);
        // Descarta el estado cacheado del vehículo y lee la fila confirmada más reciente.
        em.refresh(r.vehiculo);
        // Devuelve la reserva sincronizada con la base de datos.
        return r;
    }

    // Busca una entidad Reserva por su UUID primario o lanza ApiError 404 RESERVA_NO_ENCONTRADA.
    private Reserva buscar(String id) {
        // Ejecuta findById en ReservaRepository y traduce ausencia a excepción de dominio 404.
        return reservas.findById(id).orElseThrow(() ->
            new ApiError(404, "RESERVA_NO_ENCONTRADA", "Reserva inexistente"));
    }

    // Método auxiliar de fábrica que construye un ApiError HTTP 409 Conflict con código CONFLICTO_RESERVA.
    private ApiError conflicto(String detalle) {
        // Retorna la excepción controlada que ApiExceptionHandler serializará como RFC 7807.
        return new ApiError(409, "CONFLICTO_RESERVA", detalle);
    }
}
