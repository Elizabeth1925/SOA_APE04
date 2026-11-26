/**
 * Archivo: UsuarioApiRepository.java
 * Responsabilidad arquitectónica:
 *   Interfaz de repositorio Spring Data JPA para la entidad UsuarioApi (clave
 *   primaria Integer). Permite localizar cuentas de usuario en la tabla compartida
 *   usuarios_api durante el proceso de autenticación OAuth 2.0 y bootstrap local.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Acceso a datos del modelo de usuarios de la API.
 *   - [Actividad 11]: Consulta el usuario, su hash BCrypt y su rol RBAC para la
 *     emisión de tokens JWT STATELESS en POST /oauth/token.
 */

// Identifica el paquete que contiene las interfaces de acceso a datos.
package ec.edu.uta.venta_vehiculos_api.repository;

// Importa Optional para representar búsquedas sin coincidencias de manera segura.
import java.util.Optional;

// Importa JpaRepository para heredar persistencia CRUD estándar sobre MariaDB.
import org.springframework.data.jpa.repository.JpaRepository;

// Importa la entidad UsuarioApi administrada por este repositorio.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

// [Actividad 1] [Actividad 11] Repositorio Spring Data JPA para UsuarioApi con clave primaria Integer.
public interface UsuarioApiRepository
        extends JpaRepository<UsuarioApi, Integer> {

    // [Actividad 11] Busca una cuenta por su nombre de usuario único para autenticarla en UsuarioApiService o evitar duplicados en LocalUsers.
    Optional<UsuarioApi> findByUsername(String username);
}
