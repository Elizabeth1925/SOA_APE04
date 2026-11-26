/**
 * Archivo: LocalUsers.java
 * Responsabilidad arquitectónica:
 *   Configuración condicional activada únicamente bajo el perfil Spring "local"
 *   (@Profile("local")) que provee un CommandLineRunner para inicializar un usuario
 *   de arranque en la tabla usuarios_api usando propiedades externas, codificando su
 *   contraseña con BCrypt y validando que su rol pertenezca a la matriz RBAC.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 2]: Demuestra externalización estricta de credenciales de bootstrap
 *     mediante propiedades (app.bootstrap.*) sin incrustar contraseñas en código fuente.
 *   - [Actividad 11]: Asegura que todo usuario inicializado cuente con hash BCrypt y
 *     un rol RBAC válido (ADMIN, OPERADOR o CONSULTA) antes de emitirse tokens JWT.
 */

// Declara el paquete que agrupa las clases de configuración y soporte de seguridad.
package ec.edu.uta.venta_vehiculos_api.security;

// Importa CommandLineRunner para ejecutar lógica puntual inmediatamente después de iniciar Spring Boot.
import org.springframework.boot.CommandLineRunner;
// Importa @Configuration, @Profile y @Bean para la declaración condicional de beans.
import org.springframework.context.annotation.*;
// Importa @Value para leer el usuario, contraseña y rol inicial desde propiedades externas.
import org.springframework.beans.factory.annotation.Value;
// Importa PasswordEncoder (BCryptPasswordEncoder) para almacenar contraseñas hasheadas.
import org.springframework.security.crypto.password.PasswordEncoder;
// Importa el repositorio JPA de cuentas de usuario de la API.
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;
// Importa la entidad JPA UsuarioApi que se insertará si aún no existe.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

// [Actividad 2] Marca la clase como fuente de definiciones de beans de configuración Spring.
@Configuration
// [Actividad 2] [Actividad 11] Restringe la carga de este bean exclusivamente cuando el perfil activo incluye "local".
@Profile("local")
// Clase auxiliar de inicialización controlada de usuario administrador/local sin secretos en el repositorio.
public class LocalUsers {

    // [Actividad 2] [Actividad 11] Registra un CommandLineRunner que siembra de forma idempotente un usuario inicial.
    @Bean 
    CommandLineRunner usuarioInicial(
            // Repositorio JPA para consultar e insertar cuentas en usuarios_api.
            UsuarioApiRepository repo,
            // Codificador BCrypt inyectado desde SecurityConfig.
            PasswordEncoder encoder,
            // Nombre del usuario inicial recibido únicamente desde configuración externa.
            @Value("${app.bootstrap.user}") String user,
            // Contraseña temporal de arranque recibida desde configuración externa (nunca hardcodeada).
            @Value("${app.bootstrap.password}") String password,
            // Rol RBAC inicial con valor predeterminado ADMIN si no se especifica explícitamente.
            @Value("${app.bootstrap.role:ADMIN}") String rol) {

        // Retorna la expresión lambda ejecutada automáticamente tras levantar el contexto Spring.
        return args -> {
            // [Actividad 11] Comprueba que el rol configurado forme parte de los únicos tres roles RBAC permitidos.
            if (!java.util.List.of("ADMIN", "OPERADOR", "CONSULTA").contains(rol)) {
                // Aborta el arranque si se intentó configurar un rol no reconocido.
                throw new IllegalArgumentException("Rol inicial inválido");
            }

            // Verifica de forma idempotente si el nombre de usuario aún no existe en la tabla usuarios_api.
            if (repo.findByUsername(user).isEmpty()) {
                // Crea una nueva instancia de la entidad UsuarioApi.
                UsuarioApi u = new UsuarioApi(); 
                // Asigna el nombre de usuario externo.
                u.setUsername(user);
                // [Actividad 11] Codifica la contraseña con BCrypt antes de persistirla en MariaDB.
                u.setPassword(encoder.encode(password)); 
                // Asigna el rol RBAC validado.
                u.setRol(rol);

                // Persiste la nueva cuenta de usuario en la base de datos compartida.
                repo.save(u);
            }
        };
    }
}
