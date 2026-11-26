/**
 * Archivo: AuthController.java
 * Responsabilidad arquitectónica:
 *   Controlador REST público (/oauth/token) que implementa el flujo de emisión de
 *   tokens OAuth 2.0 (grant_type=password) con doble verificación: credenciales de
 *   aplicación cliente vía HTTP Basic Auth y credenciales de usuario contra MariaDB
 *   (BCrypt) mediante UsuarioApiService. Emite Access Token y Refresh Token JWT
 *   100% STATELESS con el rol RBAC embebido.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Contrato REST de autenticación de usuarios de la API.
 *   - [Actividad 3]: Permite obtener tokens desde cualquiera de las dos instancias
 *     equivalentes (Instancia A :8080 o Instancia B :8082) detrás de NGINX 3:1, ya que
 *     ambas comparten la misma clave HMAC-SHA y la misma tabla usuarios_api.
 *   - [Actividad 11]: Núcleo emisor de seguridad OAuth 2.0 / JWT STATELESS; incorpora
 *     el rol RBAC (ADMIN, OPERADOR o CONSULTA) en los claims del JWT y maneja errores
 *     de decodificación Base64 de forma segura devolviendo HTTP 401/400 controlados.
 */

// Identifica el paquete donde se declaran los endpoints HTTP de la aplicación.
package ec.edu.uta.venta_vehiculos_api.controller;

// Importa UTF-8 para decodificar los bytes del encabezado HTTP Basic a cadena de texto.
import java.nio.charset.StandardCharsets;
// Importa el decodificador Base64 estándar de Java para leer el encabezado Basic Auth.
import java.util.Base64;
// Importa Map para construir respuestas JSON compactas de error 400/401.
import java.util.Map;

// Importa @Value para inyectar client-id y client-secret desde configuración externa.
import org.springframework.beans.factory.annotation.Value;
// Importa HttpHeaders para referenciar el nombre estándar del encabezado "Authorization".
import org.springframework.http.HttpHeaders;
// Importa HttpStatus para devolver 401 UNAUTHORIZED y 400 BAD_REQUEST de manera explícita.
import org.springframework.http.HttpStatus;
// Importa ResponseEntity para combinar código HTTP y cuerpo JSON en cada respuesta.
import org.springframework.http.ResponseEntity;
// Importa @PostMapping para exponer POST /oauth/token.
import org.springframework.web.bind.annotation.PostMapping;
// Importa @RequestHeader para extraer el encabezado HTTP Authorization.
import org.springframework.web.bind.annotation.RequestHeader;
// Importa @RequestMapping para fijar el prefijo de ruta /oauth.
import org.springframework.web.bind.annotation.RequestMapping;
// Importa @RequestParam para leer parámetros x-www-form-urlencoded (grant_type, username, password, idapli).
import org.springframework.web.bind.annotation.RequestParam;
// Importa @RestController para registrar la clase como controlador REST JSON.
import org.springframework.web.bind.annotation.RestController;

// Importa el DTO TokenResponse que formatea la respuesta estándar OAuth 2.0.
import ec.edu.uta.venta_vehiculos_api.dto.TokenResponse;
// Importa JwtService encargado de firmar los tokens Access y Refresh con HMAC-SHA.
import ec.edu.uta.venta_vehiculos_api.security.JwtService;

// Importa la entidad UsuarioApi para obtener el rol RBAC persistido tras autenticar.
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;
// Importa UsuarioApiService encargado de validar usuario y hash BCrypt contra MariaDB.
import ec.edu.uta.venta_vehiculos_api.service.UsuarioApiService;


// [Actividad 11] Declara la clase como controlador REST del punto de emisión OAuth 2.0.
@RestController
// [Actividad 11] Define el prefijo base /oauth (ruta pública habilitada en SecurityConfig).
@RequestMapping("/oauth")
// Controlador responsable de emitir Access Token y Refresh Token JWT STATELESS.
public class AuthController {


    // [Actividad 11] Servicio inmutable encargado de generar los JWT firmados con HMAC-SHA compartida.
    private final JwtService jwtService;

    // [Actividad 11] Servicio inmutable que valida username y contraseña BCrypt contra la tabla usuarios_api.
    private final UsuarioApiService usuarioApiService;

    // [Actividad 2] [Actividad 11] Inyecta el identificador del cliente OAuth desde app.security.client-id.
    @Value("${app.security.client-id}")
    private String clientId;


    // [Actividad 2] [Actividad 11] Inyecta el secreto del cliente OAuth desde app.security.client-secret (sin secretos embebidos).
    @Value("${app.security.client-secret}")
    private String clientSecret;


    // Constructor con inyección de dependencias de JwtService y UsuarioApiService.
    public AuthController(
            // Servicio que construye y firma los tokens JWT emitidos por la API.
            JwtService jwtService,
            // Servicio que autentica las credenciales del usuario final contra MariaDB.
            UsuarioApiService usuarioApiService) {

        // Conserva la instancia de JwtService en el atributo inmutable.
        this.jwtService = jwtService;
        // Conserva la instancia de UsuarioApiService en el atributo inmutable.
        this.usuarioApiService = usuarioApiService;
    }


    // [Actividad 3] [Actividad 11] Expone POST /oauth/token (permitAll en SecurityConfig) para emitir JWT STATELESS.
    @PostMapping("/token")
    // Valida cliente Basic Auth, grant_type=password y usuario en BD; retorna TokenResponse (200) o error (400/401).
    public ResponseEntity<?> token(

            // Lee el encabezado Authorization: Basic <base64(client_id:client_secret)> (opcional para retornar 401 limpio si falta).
            @RequestHeader(
                    value = HttpHeaders.AUTHORIZATION,
                    required = false
            )
            String authorization,

            // Lee el parámetro obligatorio grant_type del formulario x-www-form-urlencoded.
            @RequestParam("grant_type")
            String grantType,

            // Lee el nombre de usuario enviado en el formulario de inicio de sesión.
            @RequestParam("username")
            String username,

            // Lee la contraseña enviada en el formulario para compararla contra el hash BCrypt persistido.
            @RequestParam("password")
            String password,

            // Lee el parámetro opcional idapli que identifica la aplicación solicitante (si es null se usa "92").
            @RequestParam(
                    value = "idapli",
                    required = false
            )
            String idapli) {


        // [Actividad 11] PRIMERA VALIDACIÓN: verifica las credenciales Basic Auth del cliente capturando Base64 mal formado.
        if (!clienteSeguro(authorization)) {

            // Construye respuesta HTTP 401 Unauthorized cuando el cliente OAuth no está autorizado.
            return ResponseEntity
                    // Establece el código de estado 401 Unauthorized.
                    .status(HttpStatus.UNAUTHORIZED)
                    // Serializa el mapa JSON con indicador de fallo y mensaje descriptivo.
                    .body(Map.of(
                            // Indica resultado fallido de autenticación.
                            "success", false,
                            // Informa rechazo de credenciales de aplicación cliente.
                            "mensaje", "Cliente no autorizado"
                    ));
        }


        // [Actividad 11] SEGUNDA VALIDACIÓN: restringe el flujo OAuth únicamente a grant_type="password".
        if (!"password".equals(grantType)) {

            // Construye respuesta HTTP 400 Bad Request si el cliente solicita un grant_type no soportado.
            return ResponseEntity
                    // Establece el código de estado 400 Bad Request.
                    .status(HttpStatus.BAD_REQUEST)
                    // Serializa el mapa JSON indicando tipo de concesión no soportado.
                    .body(Map.of(
                            // Indica resultado fallido de la solicitud.
                            "success", false,
                            // Explica el motivo del rechazo.
                            "mensaje", "grant_type no soportado"
                    ));
        }


        // [Actividad 11] TERCERA VALIDACIÓN: autentica al usuario contra usuarios_api (BCrypt) reemplazando la validación didáctica anterior.
        /* 
        if (!usuarioValido(username, password)) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "success", false,
                            "mensaje",
                            "Usuario o contraseña incorrectos"
                    ));
        }
        */
       // Declara la variable que almacenará la entidad UsuarioApi autenticada (con su rol RBAC).
       UsuarioApi usuario;
       // Invoca la capa de servicio para comprobar existencia del usuario y coincidencia del hash BCrypt.
       try {

       // [Actividad 11] Ejecuta la autenticación contra MariaDB y recupera la entidad completa con su rol.
       usuario =
               usuarioApiService.autenticar(
                       // Nombre de usuario provisto en la petición.
                       username,
                       // Contraseña provista en la petición para verificación BCrypt.
                       password
               );

       // Captura el rechazo de credenciales inválidas lanzado por UsuarioApiService.
       } catch (IllegalArgumentException e) {

       // Devuelve HTTP 401 Unauthorized con mensaje uniforme que impide enumerar usuarios existentes.
       return ResponseEntity
               // Establece el estado 401 Unauthorized.
               .status(HttpStatus.UNAUTHORIZED)
               // Construye el cuerpo JSON con mensaje genérico de autenticación fallida.
               .body(Map.of(
                       // Indica que el inicio de sesión falló.
                       "success", false,
                        // Mensaje genérico sin revelar si falló el usuario o la contraseña.
                        "mensaje",
                        "Usuario o contraseña incorrectos"
                ));
        }

        // [Actividad 11] Define el alcance lógico (claim "scope") que portarán los tokens de esta API.
        String scope = "vehiculos";


        // Resuelve el identificador de aplicación: asigna "92" por defecto si no se envió el parámetro idapli.
        String idAplicacion =
                idapli == null ? "92" : idapli;


        // [Actividad 3] [Actividad 11] Genera el Access Token JWT STATELESS firmado con HMAC-SHA compartida entre instancias A y B.
        String accessToken =
                jwtService.generarAccessToken(
                        // Claim "sub": usuario autenticado.
                        username,
                        // Claim "idapli": identificador de aplicación cliente.
                        idAplicacion,
                        // Claim "scope": alcance funcional "vehiculos".
                        scope,
                        // Claim "rol": rol RBAC persistido (ADMIN, OPERADOR o CONSULTA).
                        usuario.getRol()
                );


        // [Actividad 11] Genera el Refresh Token JWT STATELESS con claim "tipo"="refresh" y mayor tiempo de expiración.
        String refreshToken =
                jwtService.generarRefreshToken(
                        // Claim "sub": usuario autenticado.
                        username,
                        // Claim "idapli": identificador de aplicación cliente.
                        idAplicacion,
                        // Claim "scope": alcance funcional "vehiculos".
                        scope,
                        // Claim "rol": rol RBAC persistido.
                        usuario.getRol()
                );


        // [Actividad 11] Construye el DTO estándar OAuth 2.0 con ambos tokens y metadatos de expiración.
        TokenResponse respuesta =
                new TokenResponse(
                        // Campo access_token: JWT usado en Authorization: Bearer.
                        accessToken,
                        // Campo token_type: esquema estándar "bearer".
                        "bearer",
                        // Campo refresh_token: JWT de renovación.
                        refreshToken,
                        // Campo expires_in: vigencia del Access Token expresada en segundos.
                        jwtService
                                .getAccessTokenExpiresInSeconds(),
                        // Campo scope: alcance autorizado ("vehiculos").
                        scope
                );


        // Devuelve HTTP 200 OK serializando TokenResponse a JSON.
        return ResponseEntity.ok(respuesta);
    }


    // [Actividad 11] Envoltorio defensivo que captura IllegalArgumentException de Base64 mal formado evitando errores 500.
    private boolean clienteSeguro(String authorization) {
        // Intenta ejecutar la validación de Basic Auth y convierte cualquier fallo de decodificación en false (401).
        try {
            // Delega la comprobación del encabezado Basic Auth al método clienteValido.
            return clienteValido(authorization);
        // Captura cadenas Base64 corruptas o caracteres ilegales en el encabezado Authorization.
        } catch (IllegalArgumentException ex) {
            // Responde false para que el endpoint retorne HTTP 401 controlado.
            return false;
        }
    }

    // [Actividad 11] Decodifica y valida el encabezado HTTP Basic Auth contra app.security.client-id y client-secret.
    private boolean clienteValido(
            String authorization) {


        // Rechaza inmediatamente si falta el encabezado Authorization o no comienza con el esquema "Basic ".
        if (authorization == null ||
                !authorization.startsWith("Basic ")) {
            // Indica credenciales de cliente ausentes o con esquema incorrecto.
            return false;
        }


        // Extrae la porción Base64 eliminando los 6 caracteres iniciales correspondientes al prefijo "Basic ".
        String base64 =
                authorization.substring(6);


        // Decodifica la carga Base64 a texto UTF-8 con el formato esperado "<client_id>:<client_secret>".
        String credenciales =
                new String(
                        // Convierte el segmento Base64 a su arreglo de bytes original.
                        Base64
                                .getDecoder()
                                .decode(base64),
                        // Interpreta los bytes decodificados bajo codificación UTF-8.
                        StandardCharsets.UTF_8
                );


        // Divide las credenciales en máximo 2 fragmentos usando ":" como separador para respetar secretos con ":".
        String[] partes =
                credenciales.split(":", 2);


        // Comprueba que existan exactamente el identificador y el secreto del cliente.
        if (partes.length != 2) {
            // Rechaza formatos mal construidos que no contengan separador ":".
            return false;
        }


        // Recupera el identificador del cliente OAuth enviado en la petición.
        String usuarioCliente =
                partes[0];


        // Recupera el secreto del cliente OAuth enviado en la petición.
        String claveCliente =
                partes[1];


        // Verifica coincidencia exacta simultánea contra el clientId y clientSecret externalizados.
        return clientId.equals(usuarioCliente) &&
                clientSecret.equals(claveCliente);
    }


    // Bloque legado deshabilitado: reemplazado por UsuarioApiService con persistencia en MariaDB y hashes BCrypt.
    /* 
    private boolean usuarioValido(
            String username,
            String password) {
        return "9999999999".equals(username) &&
                "clave_usuario".equals(password);
    }
    */
}
