package ec.edu.uta.venta_vehiculos_api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ec.edu.uta.venta_vehiculos_api.model.Reserva;

// Repositorio Spring Data JPA para la entidad Reserva con clave primaria de tipo String
public interface ReservaRepository extends JpaRepository<Reserva, String> {

    // Busca una reserva por su identificador único de operación (idempotencia)
    Optional<Reserva> findByOperacionId(String operacionId);

    // Comprueba si existe al menos una reserva asociada a un vehículo dado su ID primario
    boolean existsByVehiculo_IdVehiculo(Integer id);
}
