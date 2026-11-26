/**
 * Archivo: UsuarioApiService.java
 * Responsabilidad arquitectónica:
 *   Capa de servicio encargada de autenticar cuentas de usuario de la API contra la
 *   tabla compartida usuarios_api en MariaDB, verificando de forma segura la
 *   contraseña en texto contra su hash BCrypt almacenado mediante PasswordEncoder.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Implementa la lógica de negocio asociada al modelo de usuarios.
 *   - [Actividad 11]: Soporta el flujo OAuth 2.0 en AuthController (POST /oauth/token),
 *     validando credenciales con BCrypt sin comparar texto plano y devolviendo un
 *     mensaje genérico de fallo para mitigar ataques de enumeración de cuentas.
 */

// Identifica el paquete de servicios donde reside la lógica de negocio y autenticación.
package ec.edu.uta.venta_vehiculos_api.service;

// Importa PasswordEncoder (implementado por BCryptPasswordEncoder) para verificar hashes de contraseña.
import org.springframework.security.crypto.password.PasswordEncoder;
// Importa @Service para registrar la clase como bean administrado por Spring.
import org.springframework.stereotype.Service;

// Importa la entidad UsuarioApi que contiene el hash BCrypt y el rol RBAC del usuario.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;
// Importa UsuarioApiRepository para consultar usuarios por username en MariaDB.
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;

// [Actividad 1] [Actividad 11] Registra el servicio de autenticación de usuarios para su inyección en AuthController.
@Service
// Agrupa la lógica de validación de credenciales contra base de datos con BCrypt.
public class UsuarioApiService {

    // Repositorio inmutable que accede a la tabla usuarios_api compartida entre instancias A y B.
    private final UsuarioApiRepository repository;
    // Codificador criptográfico inmutable (BCrypt) inyectado desde SecurityConfig.
    private final PasswordEncoder passwordEncoder;

    // Constructor con inyección de dependencias de repositorio y codificador de contraseñas.
    public UsuarioApiService(
            // Repositorio JPA de cuentas de usuario.
            UsuarioApiRepository repository,
            // Implementación BCryptPasswordEncoder configurada en SecurityConfig.
            PasswordEncoder passwordEncoder) {

        // Guarda el repositorio en el atributo de instancia.
        this.repository = repository;
        // Guarda el codificador BCrypt en el atributo de instancia.
        this.passwordEncoder = passwordEncoder;
    }

    // [Actividad 11] Autentica username y password contra usuarios_api y devuelve la entidad con su rol RBAC.
    public UsuarioApi autenticar(
            // Nombre de usuario recibido en POST /oauth/token.
            String username,
            // Contraseña recibida en la petición para compararla contra el hash BCrypt persistido.
            String password) {

        // Busca el usuario por su nombre único en MariaDB; si no existe lanza IllegalArgumentException genérico.
        UsuarioApi usuario =
                repository.findByUsername(username)
                        // Traduce la ausencia del usuario al mismo mensaje genérico para evitar enumeración de cuentas.
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario o contraseña incorrectos"
                                )
                        );

        // [Actividad 11] Verifica criptográficamente la contraseña recibida contra el hash BCrypt almacenado.
        if (!passwordEncoder.matches(
                // Contraseña suministrada en el intento de autenticación.
                password,
                // Hash BCrypt persistido en la columna password de usuarios_api.
                usuario.getPassword())) {

            // Rechaza la autenticación con el mismo mensaje uniforme (AuthController lo traduce a HTTP 401).
            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos"
            );
        }

        // Devuelve la entidad UsuarioApi verificada para que AuthController incorpore su rol en el JWT.
        return usuario;
    }
}
