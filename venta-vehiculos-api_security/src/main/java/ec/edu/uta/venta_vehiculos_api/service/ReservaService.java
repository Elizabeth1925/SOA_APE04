package ec.edu.uta.venta_vehiculos_api.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.exception.ApiError;
import ec.edu.uta.venta_vehiculos_api.model.*;
import ec.edu.uta.venta_vehiculos_api.repository.*;

// Servicio que implementa la lógica transaccional de reservas
@Service
// Nivel de aislamiento READ_COMMITTED para evitar lecturas sucias durante concurrencia
@Transactional(isolation=Isolation.READ_COMMITTED)
public class ReservaService {

    private final VehiculoRepository vehiculos;
    private final ReservaRepository reservas;

    // Inyección de dependencias para los repositorios de vehículos y reservas
    public ReservaService(VehiculoRepository v, ReservaRepository r) {
        vehiculos = v;
        reservas = r;
    }

    // Crea una nueva reserva para un vehículo dado su ID y un código de operación
    public ReservaResponse reservar(int id, String op) {
        // Bloquea pesimistamente el vehículo para asegurar concurrencia
        Vehiculo v = vehiculos.bloquear(id).orElseThrow(() ->
            new ApiError(404, "VEHICULO_NO_ENCONTRADO", "Vehículo inexistente"));

        // Idempotencia: si ya existe una reserva previa con esta misma operacionId
        var anterior = reservas.findByOperacionId(op);
        if (anterior.isPresent()) {
            Reserva r = anterior.get();
            // Verifica que la operación previa no haya sido registrada para un vehículo diferente
            if (!r.vehiculo.getIdVehiculo().equals(id))
                throw conflicto("La operación pertenece a otro vehículo");

            // Si coincide, devuelve la reserva existente sin duplicar
            return ReservaResponse.de(r);
        }

        // Regla de negocio: el vehículo debe encontrarse en estado DISPONIBLE
        if (!"DISPONIBLE".equals(v.getEstado()))
            throw conflicto("El vehículo no está disponible");

        // Construye y persiste la nueva reserva
        Reserva r = new Reserva();
        r.id = UUID.randomUUID().toString();
        r.operacionId = op;
        r.vehiculo = v;
        r.precio = v.getPrecio();
        r.moneda = "USD";
        r.estado = "RESERVADA";

        // Actualiza el estado del vehículo a RESERVADO
        v.setEstado("RESERVADO");
        reservas.saveAndFlush(r);

        return ReservaResponse.de(r);
    }

    // Consulta de sólo lectura para obtener una reserva por su ID
    @Transactional(readOnly=true)
    public ReservaResponse obtener(String id) {
        return ReservaResponse.de(buscar(id));
    }

    // Consulta de sólo lectura para buscar la reserva vinculada a una operacionId
    @Transactional(readOnly=true)
    public ReservaResponse porOperacion(String op) {
        return ReservaResponse.de(reservas.findByOperacionId(op)
            .orElseThrow(() -> new ApiError(404, "RESERVA_NO_ENCONTRADA",
                "La operación no tiene reserva")));
    }

    // Confirma la reserva convirtiéndola en venta efectiva del vehículo
    public ReservaResponse confirmar(String id) {
        Reserva r = bloquear(id);

        // No se puede confirmar una reserva previamente cancelada/liberada
        if ("LIBERADA".equals(r.estado))
            throw conflicto("La reserva fue liberada");

        // Si está en estado RESERVADA se procede a confirmarla
        if ("RESERVADA".equals(r.estado)) {
            if (!"RESERVADO".equals(r.vehiculo.getEstado()))
                throw conflicto("Estado del vehículo inconsistente");

            // Cambia el estado de la reserva a CONFIRMADA y el del vehículo a VENDIDO
            r.estado = "CONFIRMADA";
            r.vehiculo.setEstado("VENDIDO");
        }

        return ReservaResponse.de(r);
    }

    // Libera / cancela la reserva devolviendo el vehículo a DISPONIBLE
    public void liberar(String id) {
        Reserva r = bloquear(id);

        // Regla: no se permite liberar una reserva que ya fue confirmada (vendida)
        if ("CONFIRMADA".equals(r.estado))
            throw conflicto("No se libera una reserva confirmada");

        // Si estaba reservada, se libera y el vehículo vuelve a estar DISPONIBLE
        if ("RESERVADA".equals(r.estado)) {
            r.estado = "LIBERADA";
            r.vehiculo.setEstado("DISPONIBLE");
        }
    }

    // Bloquea pesimistamente el vehículo asociado a la reserva para evitar condiciones de carrera
    private Reserva bloquear(String id) {
        Reserva r = buscar(id);
        vehiculos.bloquear(r.vehiculo.getIdVehiculo()).orElseThrow();
        return refrescar(r);
    }

    // Inyección del contexto de persistencia JPA
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

    // Sincroniza las entidades desde la base de datos asegurando datos actualizados
    private Reserva refrescar(Reserva r) {
        em.refresh(r);
        em.refresh(r.vehiculo);
        return r;
    }

    // Busca la entidad Reserva por ID o lanza ApiError 404
    private Reserva buscar(String id) {
        return reservas.findById(id).orElseThrow(() ->
            new ApiError(404, "RESERVA_NO_ENCONTRADA", "Reserva inexistente"));
    }

    // Genera un ApiError con estado 409 Conflict
    private ApiError conflicto(String detalle) {
        return new ApiError(409, "CONFLICTO_RESERVA", detalle);
    }
}
