// Identifica el paquete donde se declaran las entidades persistidas del sistema.
package ec.edu.uta.venta_vehiculos_api.model;

// Importa las anotaciones JPA para mapear esta clase y sus campos a una tabla.
import jakarta.persistence.*;

// Declara esta clase como una entidad que JPA puede guardar y consultar.
@Entity
// Asocia la entidad con la tabla usuarios_api de la base de datos.
@Table(name = "usuarios_api")
// Representa una cuenta de usuario habilitada para acceder a la API.
public class UsuarioApi {

    // Marca el campo id como clave primaria de cada registro.
    @Id
    // Configura la base de datos para generar automáticamente el identificador.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Guarda el identificador único del usuario.
    private Integer id;

    // Exige que el nombre de usuario tenga valor y que no se repita en la tabla.
    @Column(nullable = false, unique = true)
    // Guarda el nombre de inicio de sesión del usuario.
    private String username;

    // Exige que la contraseña tenga un valor antes de persistir el usuario.
    @Column(nullable = false)
    // Guarda la contraseña codificada del usuario.
    private String password;

    // Exige que cada usuario tenga asignado un rol.
    @Column(nullable = false)
    // Guarda el rol que determina los permisos del usuario.
    private String rol;

    // Constructor vacío requerido por JPA para crear instancias de la entidad.
    public UsuarioApi() {
    }

    // Devuelve el identificador del usuario.
    public Integer getId() {
        return id;
    }

    // Asigna el identificador del usuario.
    public void setId(Integer id) {
        this.id = id;
    }

    // Devuelve el nombre de usuario.
    public String getUsername() {
        return username;
    }

    // Actualiza el nombre de usuario.
    public void setUsername(String username) {
        this.username = username;
    }

    // Devuelve la contraseña codificada.
    public String getPassword() {
        return password;
    }

    // Actualiza la contraseña codificada.
    public void setPassword(String password) {
        this.password = password;
    }

    // Devuelve el rol asignado.
    public String getRol() {
        return rol;
    }

    // Asigna o actualiza el rol del usuario.
    public void setRol(String rol) {
        this.rol = rol;
    }
}