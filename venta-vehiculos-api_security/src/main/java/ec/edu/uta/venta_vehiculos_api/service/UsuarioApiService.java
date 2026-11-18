// Identifica el paquete de servicios donde se encuentra la lógica de autenticación.
package ec.edu.uta.venta_vehiculos_api.service;

// Importa el contrato para verificar contraseñas codificadas sin compararlas en texto plano.
import org.springframework.security.crypto.password.PasswordEncoder;
// Permite registrar esta clase como un servicio administrado por Spring.
import org.springframework.stereotype.Service;

// Importa la entidad que contiene los datos de usuario.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;
// Importa el repositorio usado para consultar los usuarios persistidos.
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;

// Registra el servicio para que Spring pueda inyectarlo en otros componentes.
@Service
// Agrupa la lógica relacionada con la autenticación de usuarios de la API.
public class UsuarioApiService {

    // Repositorio inmutable utilizado para buscar usuarios en la base de datos.
    private final UsuarioApiRepository repository;
    // Codificador utilizado para comparar la contraseña recibida con la almacenada.
    private final PasswordEncoder passwordEncoder;

    // Constructor de inyección de dependencias para recibir el repositorio y el codificador.
    public UsuarioApiService(
            // Repositorio con las operaciones de persistencia de usuarios.
            UsuarioApiRepository repository,
            // Implementación configurada para verificar contraseñas codificadas.
            PasswordEncoder passwordEncoder) {

        // Conserva el repositorio recibido para usarlo en las operaciones del servicio.
        this.repository = repository;
        // Conserva el codificador recibido para validar credenciales.
        this.passwordEncoder = passwordEncoder;
    }

    // Busca al usuario por su nombre y comprueba la contraseña antes de autenticarlo.
    public UsuarioApi autenticar(
            // Nombre de usuario enviado en la solicitud de autenticación.
            String username,
            // Contraseña en texto recibida que se comparará con el valor codificado.
            String password) {

        // Consulta el usuario por nombre y obtiene su entidad si existe.
        UsuarioApi usuario =
                repository.findByUsername(username)
                        // Si el usuario no existe, comunica que las credenciales no son válidas.
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Usuario o contraseña incorrectos"
                                )
                        );

        // Comprueba que la contraseña recibida corresponda a la contraseña guardada.
        if (!passwordEncoder.matches(
                // Contraseña recibida en la solicitud.
                password,
                // Contraseña codificada asociada al usuario encontrado.
                usuario.getPassword())) {

            // Rechaza el inicio de sesión sin revelar cuál credencial fue incorrecta.
            throw new IllegalArgumentException(
                    "Usuario o contraseña incorrectos"
            );
        }

        // Devuelve la entidad del usuario autenticado para que el controlador use sus datos.
        return usuario;
    }
}