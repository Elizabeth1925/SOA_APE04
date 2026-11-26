/**
 * Archivo: JwtService.java
 * Responsabilidad arquitectónica:
 *   Servicio criptográfico responsable de emitir (firmar) y validar tokens JWT
 *   mediante firma simétrica HMAC-SHA (JJWT) usando la clave secreta externa
 *   app.security.jwt-secret. Encapsula la construcción de claims estándar (sub,
 *   iat, exp) y personalizados (idapli, scope, rol, tipo) tanto para Access Token
 *   como para Refresh Token.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 3]: Permite operación 100% STATELESS detrás de NGINX en balanceo
 *     ponderado 3:1; al compartir exactamente la misma clave HMAC-SHA entre
 *     Instancia A (:8080) e Instancia B (:8082), un token emitido por la Instancia A
 *     es validado sin estado por la Instancia B y viceversa.
 *   - [Actividad 11]: Pilar criptográfico del esquema OAuth 2.0 / JWT STATELESS;
 *     asegura integridad, autenticidad, vigencia temporal y propagación del rol RBAC
 *     (ADMIN, OPERADOR, CONSULTA) y del tipo de token ("access" vs "refresh").
 */

// Identifica el paquete donde se implementa el soporte criptográfico y de seguridad JWT.
package ec.edu.uta.venta_vehiculos_api.security;


// Importa StandardCharsets.UTF_8 para derivar bytes deterministas de la clave secreta HMAC.
import java.nio.charset.StandardCharsets;
// Importa Date para establecer las marcas temporales iat (emisión) y exp (expiración).
import java.util.Date;
// Importa Map para declarar el conjunto de claims personalizados embebidos en el payload JWT.
import java.util.Map;

// Importa SecretKey del API JCA para representar la clave simétrica HMAC-SHA.
import javax.crypto.SecretKey;

// Importa @Value para inyectar secreto y vigencias desde configuración externa.
import org.springframework.beans.factory.annotation.Value;
// Importa @Service para registrar la clase como bean singleton inyectable en AuthController y JwtAuthenticationFilter.
import org.springframework.stereotype.Service;

// Importa Claims de JJWT para representar los atributos verificados del payload JWT.
import io.jsonwebtoken.Claims;
// Importa Jwts, fachada principal de JJWT para construir (builder) y verificar (parser) tokens.
import io.jsonwebtoken.Jwts;
// Importa Keys de JJWT para construir una SecretKey HMAC-SHA válida a partir de bytes del secreto.
import io.jsonwebtoken.security.Keys;


// [Actividad 3] [Actividad 11] Registra el servicio criptográfico JWT en el contenedor Spring.
@Service
// Genera y verifica criptográficamente los JWT STATELESS compartidos entre instancias A y B.
public class JwtService {


    // [Actividad 2] [Actividad 3] [Actividad 11] Secreto simétrico HMAC-SHA compartido entre Instancia A (:8080) e Instancia B (:8082).
    @Value("${app.security.jwt-secret}")
    private String jwtSecret;


    // [Actividad 11] Tiempo de vigencia configurado en minutos para el Access Token (ej. 30 min).
    @Value("${app.security.access-token-minutes}")
    private long accessTokenMinutes;


    // [Actividad 11] Tiempo de vigencia configurado en minutos para el Refresh Token (ej. 1200 min).
    @Value("${app.security.refresh-token-minutes}")
    private long refreshTokenMinutes;


    // [Actividad 3] [Actividad 11] Deriva la SecretKey HMAC-SHA utilizada tanto para firmar como para verificar tokens.
    private SecretKey getSigningKey() {

        // Construye la clave criptográfica HMAC-SHA (>= 256 bits) a partir de los bytes UTF-8 del secreto compartido.
        return Keys.hmacShaKeyFor(

                // Convierte el secreto de texto en arreglo de bytes UTF-8 de manera portable entre entornos.
                jwtSecret.getBytes(StandardCharsets.UTF_8)
        );
    }


    // [Actividad 11] Genera un Access Token JWT de corta duración con claim "tipo"="access" y el rol RBAC del usuario.
    public String generarAccessToken(
            // Nombre del usuario autenticado que ocupará el claim estándar "sub".
            String username,
            // Identificador de la aplicación cliente (claim "idapli").
            String idapli,
            // Alcance funcional autorizado (claim "scope").
            String scope,
            // Rol RBAC asignado al usuario (claim "rol": ADMIN, OPERADOR o CONSULTA).
            String rol) {

        // Delega la construcción al método privado común usando la duración de Access Token y tipo "access".
        return generarToken(

                // Sujeto propietario del token.
                username,

                // Identificador de aplicación cliente.
                idapli,

                // Alcance lógico de autorización.
                scope,
                // Rol RBAC validado posteriormente por JwtAuthenticationFilter.
                rol,

                // Vigencia configurada en minutos para el Access Token.
                accessTokenMinutes,

                // Marca el JWT como token de acceso válido para consumir endpoints protegidos.
                "access"
        );
    }


    // [Actividad 11] Genera un Refresh Token JWT de mayor duración con claim "tipo"="refresh" (rechazado en endpoints de recursos).
    public String generarRefreshToken(
            // Nombre del usuario propietario del token de renovación.
            String username,
            // Identificador de la aplicación cliente asociada.
            String idapli,
            // Alcance lógico conservado en el token de renovación.
            String scope,
            // Rol RBAC del usuario.
            String rol) {

        // Delega la construcción al método privado común usando la duración de Refresh Token y tipo "refresh".
        return generarToken(

                // Sujeto propietario del token.
                username,

                // Identificador de aplicación cliente.
                idapli,

                // Alcance lógico de autorización.
                scope,
                // Rol RBAC del usuario.
                rol,

                // Vigencia configurada en minutos para el Refresh Token.
                refreshTokenMinutes,

                // Marca el JWT como "refresh" para que JwtAuthenticationFilter impida usarlo como Bearer de acceso.
                "refresh"
        );
    }


    // [Actividad 11] Método interno que ensambla cabecera, payload con claims, tiempos iat/exp y firma HMAC-SHA.
    private String generarToken(
            // Usuario propietario (claim "sub").
            String username,
            // Identificador de aplicación (claim "idapli").
            String idapli,
            // Alcance autorizado (claim "scope").
            String scope,
            // Rol RBAC del usuario (claim "rol").
            String rol,
            // Duración de validez en minutos.
            long minutos,
            // Tipo de token ("access" o "refresh").
            String tipo) {


        // Captura la marca temporal actual del sistema para el claim "iat" (Issued At).
        Date ahora = new Date();


        // Calcula el instante exacto de vencimiento sumando los minutos convertidos a milisegundos.
        Date expiracion = new Date(

                // Milisegundos actuales + (minutos * 60 segundos * 1000 milisegundos).
                ahora.getTime() + minutos * 60 * 1000
        );


        // [Actividad 11] Construye, firma y serializa el JWT compacto <HEADER>.<PAYLOAD>.<SIGNATURE>.
        return Jwts.builder()

                // Establece el claim estándar "sub" con el username autenticado.
                .subject(username)


                // Adjunta los claims personalizados exigidos por la arquitectura de seguridad.
                .claims(Map.of(

                        // Identificador de aplicación cliente.
                        "idapli", idapli,

                        // Alcance funcional del token (ej. "vehiculos").
                        "scope", scope,
                        // [Actividad 11] Rol RBAC utilizado por JwtAuthenticationFilter para construir ROLE_<rol>.
                        "rol", rol,

                        // [Actividad 11] Distingue explícitamente entre token "access" y token "refresh".
                        "tipo", tipo
                ))


                // Registra la fecha/hora de emisión en el claim estándar "iat".
                .issuedAt(ahora)


                // Registra la fecha/hora límite de validez en el claim estándar "exp".
                .expiration(expiracion)


                // [Actividad 3] [Actividad 11] Firma el JWT con la clave simétrica HMAC-SHA compartida entre instancias A y B.
                .signWith(getSigningKey())


                // Serializa el token firmado a su representación compacta Base64URL (<HEADER>.<PAYLOAD>.<SIGNATURE>).
                .compact();
    }


    // [Actividad 3] [Actividad 11] Verifica estructura, firma HMAC-SHA y expiración del JWT recibido y devuelve sus claims.
    public Claims validarToken(String token) {

        // Construye el parser verificador de JJWT configurado con la clave compartida HMAC-SHA.
        return Jwts.parser()

                // Exige que la firma coincida criptográficamente con la clave secreta compartida.
                .verifyWith(getSigningKey())

                // Finaliza la construcción del parser inmutable.
                .build()

                // Analiza el JWT compacto verificando formato, algoritmo, firma y vigencia (lanza JwtException si falla).
                .parseSignedClaims(token)

                // Extrae y devuelve el conjunto de claims verificados del payload.
                .getPayload();
    }


    // [Actividad 11] Convierte la vigencia configurada del Access Token de minutos a segundos para el campo expires_in.
    public long getAccessTokenExpiresInSeconds() {

        // Multiplica los minutos configurados por 60 conforme al estándar OAuth 2.0 (expires_in en segundos).
        return accessTokenMinutes * 60;
    }
}
