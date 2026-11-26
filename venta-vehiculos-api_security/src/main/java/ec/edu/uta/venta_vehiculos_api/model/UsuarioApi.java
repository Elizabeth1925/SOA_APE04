/**
 * Archivo: UsuarioApi.java
 * Responsabilidad arquitectónica:
 *   Entidad JPA mapeada a la tabla "usuarios_api" en el esquema compartido
 *   venta_vehiculos. Almacena las cuentas habilitadas para autenticarse en
 *   POST /oauth/token, conservando la contraseña encriptada con BCrypt y el rol
 *   RBAC asignado (ADMIN, OPERADOR o CONSULTA).
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Define el modelo persistente de usuarios de la API de vehículos.
 *   - [Actividad 11]: Soporta la autenticación OAuth 2.0 y la emisión de claims de rol
 *     RBAC (ADMIN/OPERADOR/CONSULTA) en los tokens JWT STATELESS consumidos por las
 *     instancias A (:8080) y B (:8082).
 */

// Identifica el paquete donde se declaran las entidades persistidas del sistema.
package ec.edu.uta.venta_vehiculos_api.model;

// Importa las anotaciones estándar de Jakarta Persistence (JPA) para el mapeo ORM.
import jakarta.persistence.*;

// [Actividad 1] [Actividad 11] Declara esta clase como entidad relacional gestionada por JPA/Hibernate.
@Entity
// Asocia la entidad con la tabla física "usuarios_api" del esquema venta_vehiculos.
@Table(name = "usuarios_api")
// Entidad que representa una cuenta de usuario autorizada a solicitar tokens JWT.
public class UsuarioApi {

    // Identifica el campo id como clave primaria de la tabla usuarios_api.
    @Id
    // Delega la generación autoincremental de la clave primaria a la columna AUTO_INCREMENT de MariaDB.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Identificador numérico único del usuario en la base de datos.
    private Integer id;

    // Restringe username a valores obligatorios y únicos para evitar cuentas duplicadas.
    @Column(nullable = false, unique = true)
    // Nombre de inicio de sesión que se incluye como claim "sub" en el JWT.
    private String username;

    // Exige que toda cuenta persista un hash de contraseña no nulo.
    @Column(nullable = false)
    // [Actividad 11] Almacena el hash BCrypt de la contraseña (nunca texto plano).
    private String password;

    // Exige que cada usuario tenga asignado un rol RBAC no nulo.
    @Column(nullable = false)
    // [Actividad 11] Rol de autorización del usuario: ADMIN, OPERADOR o CONSULTA.
    private String rol;

    // Constructor vacío exigido por JPA para instanciar entidades al hidratar consultas.
    public UsuarioApi() {
    }

    // Devuelve el identificador primario del usuario.
    public Integer getId() {
        // Retorna el valor del campo id.
        return id;
    }

    // Asigna o modifica el identificador primario del usuario.
    public void setId(Integer id) {
        // Guarda el argumento recibido en el campo id.
        this.id = id;
    }

    // Devuelve el nombre de inicio de sesión del usuario.
    public String getUsername() {
        // Retorna el valor del campo username.
        return username;
    }

    // Asigna o actualiza el nombre de inicio de sesión del usuario.
    public void setUsername(String username) {
        // Guarda el argumento recibido en el campo username.
        this.username = username;
    }

    // Devuelve el hash BCrypt de la contraseña almacenada.
    public String getPassword() {
        // Retorna el valor del campo password.
        return password;
    }

    // Asigna o actualiza el hash BCrypt de la contraseña del usuario.
    public void setPassword(String password) {
        // Guarda el argumento recibido en el campo password.
        this.password = password;
    }

    // [Actividad 11] Devuelve el rol RBAC asignado al usuario (ADMIN, OPERADOR o CONSULTA).
    public String getRol() {
        // Retorna el valor del campo rol.
        return rol;
    }

    // [Actividad 11] Asigna o actualiza el rol RBAC del usuario.
    public void setRol(String rol) {
        // Guarda el argumento recibido en el campo rol.
        this.rol = rol;
    }
}
