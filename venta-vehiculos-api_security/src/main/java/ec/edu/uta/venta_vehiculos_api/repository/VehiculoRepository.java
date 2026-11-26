/**
 * Archivo: VehiculoRepository.java
 * Responsabilidad arquitectónica:
 *   Interfaz de repositorio Spring Data JPA para la entidad Vehiculo (clave primaria
 *   Integer). Además de las búsquedas por placa, chasis y marca, implementa un bloqueo
 *   pesimista nativo SQL (SELECT ... FOR UPDATE) compatible con MariaDB 10.4 para
 *   serializar el acceso concurrente a una misma fila de vehículo.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Provee acceso a datos y filtrado por marca para los endpoints CRUD
 *     de VehiculoController.
 *   - [Actividad 4]: Implementa el bloqueo pesimista nativo SQL `FOR UPDATE` compatible
 *     con MariaDB 10.4 (en lugar de dialectos JPQL con `FOR NO KEY UPDATE` no soportados
 *     por MariaDB 10.4) para proteger las transiciones DISPONIBLE -> RESERVADO -> VENDIDO.
 *   - [Actividad 10 - P27]: Serializa transacciones concurrentes provenientes tanto de
 *     Instancia A (:8080) como de Instancia B (:8082) sobre la base compartida, evitando
 *     doble reserva o ediciones simultáneas sobre un mismo vehículo.
 */

// Identifica el paquete que contiene las interfaces de acceso a datos.
package ec.edu.uta.venta_vehiculos_api.repository;

// Importa Optional y List para retornar resultados individuales opcionales y colecciones.
import java.util.*;

// Importa JpaRepository y @Query de Spring Data JPA.
import org.springframework.data.jpa.repository.*;

// Importa @Param para vincular parámetros nombrados (:id) en consultas SQL nativas.
import org.springframework.data.repository.query.Param;

// Importa la entidad Vehiculo gestionada por este repositorio.
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;

// [Actividad 1] [Actividad 4] [Actividad 10 - P27] Repositorio Spring Data JPA de vehículos con clave Integer.
public interface VehiculoRepository extends JpaRepository<Vehiculo, Integer> {

    // [Actividad 1] Busca un vehículo cuya placa coincida exactamente con el valor indicado.
    Optional<Vehiculo> findByPlaca(String placa);

    // [Actividad 1] Busca un vehículo cuyo número de chasis (VIN) coincida exactamente con el valor indicado.
    Optional<Vehiculo> findByChasis(String chasis);

    // [Actividad 1] Lista vehículos cuya marca contenga la subcadena recibida sin distinguir mayúsculas ni minúsculas.
    List<Vehiculo> findByMarcaContainingIgnoreCase(String marca);

    // [Actividad 4] [Actividad 10 - P27] Ejecuta bloqueo pesimista nativo SQL FOR UPDATE compatible con MariaDB 10.4 sobre la fila del vehículo.
    @Query(value = "SELECT * FROM vehiculos WHERE id_vehiculo = :id FOR UPDATE", nativeQuery = true)
    // Adquiere un bloqueo exclusivo a nivel de fila dentro de la transacción activa hasta commit/rollback.
    Optional<Vehiculo> bloquear(@Param("id") Integer id);
}
