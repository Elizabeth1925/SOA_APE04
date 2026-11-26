/**
 * Archivo: ReservaRepository.java
 * Responsabilidad arquitectónica:
 *   Interfaz de repositorio Spring Data JPA para la entidad Reserva (clave primaria
 *   UUID de tipo String). Provee consultas derivadas para garantizar idempotencia por
 *   operacionId y proteger la integridad histórica de vehículos con reservas previas.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Soporta la búsqueda idempotente por operacionId durante la
 *     orquestación en 2 fases con ventas-api (FastAPI) y las consultas de estado.
 *   - [Actividad 10 - P27]: Accede a la tabla compartida "reservas" en MariaDB para
 *     mantener coherencia transversal entre Instancia A (:8080) e Instancia B (:8082),
 *     impidiendo eliminar vehículos que posean historial de reservas.
 */

// Declara el paquete que concentra los repositorios de acceso a datos.
package ec.edu.uta.venta_vehiculos_api.repository;

// Importa Optional para modelar resultados de búsqueda que pueden no existir sin usar null.
import java.util.Optional;
// Importa JpaRepository para heredar operaciones CRUD, paginación y flush de Spring Data JPA.
import org.springframework.data.jpa.repository.JpaRepository;
// Importa la entidad Reserva gestionada por este repositorio.
import ec.edu.uta.venta_vehiculos_api.model.Reserva;

// [Actividad 4] [Actividad 10 - P27] Repositorio Spring Data JPA para Reserva con clave primaria String (UUID).
public interface ReservaRepository extends JpaRepository<Reserva, String> {

    // [Actividad 4] [Actividad 10 - P27] Consulta una reserva por su clave única operacionId para asegurar idempotencia entre reintentos e instancias.
    Optional<Reserva> findByOperacionId(String operacionId);

    // [Actividad 1] [Actividad 10 - P27] Verifica si existen reservas asociadas al vehículo indicado (bloquea DELETE en VehiculoService si es true).
    boolean existsByVehiculo_IdVehiculo(Integer id);
}
