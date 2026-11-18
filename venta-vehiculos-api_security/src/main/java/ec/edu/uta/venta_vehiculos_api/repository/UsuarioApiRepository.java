// Identifica el paquete que contiene las interfaces de acceso a datos.
package ec.edu.uta.venta_vehiculos_api.repository;

// Importa Optional para representar de forma segura una búsqueda sin resultados.
import java.util.Optional;

// Importa la interfaz Spring Data que aporta operaciones CRUD para entidades JPA.
import org.springframework.data.jpa.repository.JpaRepository;

// Importa la entidad administrada por este repositorio.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

// Declara el repositorio y hereda las operaciones JPA para UsuarioApi con clave Integer.
public interface UsuarioApiRepository
        extends JpaRepository<UsuarioApi, Integer> {

    // Busca un usuario por su nombre y devuelve vacío si no existe.
    Optional<UsuarioApi> findByUsername(String username);
}