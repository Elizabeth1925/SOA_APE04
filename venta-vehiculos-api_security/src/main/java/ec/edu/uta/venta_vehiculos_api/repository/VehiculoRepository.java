
// Identifica el paquete que contiene las interfaces de acceso a datos.
package ec.edu.uta.venta_vehiculos_api.repository;

// Importa Optional y List para representar resultados individuales opcionales y listas.
import java.util.*;

// Importa el tipo de bloqueo pesimista que se aplica a la consulta por identificador.
import jakarta.persistence.LockModeType;

// Importa las anotaciones y contratos de repositorios JPA de Spring Data.
import org.springframework.data.jpa.repository.*;

// Permite enlazar un parámetro nombrado de una consulta declarada.
import org.springframework.data.repository.query.Param;

// Importa la entidad que este repositorio persiste y consulta.
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

// Declara el repositorio de vehículos y hereda operaciones JPA para claves Integer.
public interface VehiculoRepository

    extends JpaRepository<Vehiculo, Integer> {

    // Busca un vehículo cuya placa coincida exactamente con el valor recibido.
    Optional<Vehiculo> findByPlaca(String placa);

    // Busca un vehículo cuyo número de chasis coincida exactamente con el valor recibido.
    Optional<Vehiculo> findByChasis(String chasis);

    // Busca vehículos cuya marca contenga el texto indicado, ignorando mayúsculas y minúsculas.
    List<Vehiculo> findByMarcaContainingIgnoreCase(String marca);

    // Solicita un bloqueo de escritura para impedir modificaciones concurrentes del registro.
    @Lock(LockModeType.PESSIMISTIC_WRITE)

    // Define una consulta JPQL que selecciona un vehículo por su identificador.
    @Query("select v from Vehiculo v where v.idVehiculo = :id")

    // Enlaza el argumento id con :id y devuelve el registro bloqueado si existe.
    Optional<Vehiculo> bloquear(@Param("id") Integer id);

}