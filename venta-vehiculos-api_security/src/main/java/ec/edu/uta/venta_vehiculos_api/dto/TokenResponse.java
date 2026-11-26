/**
 * Archivo: TokenResponse.java
 * Responsabilidad arquitectónica:
 *   DTO que modela el cuerpo JSON estándar OAuth 2.0 emitido por POST /oauth/token
 *   en AuthController. Encapsula el Access Token JWT, tipo de esquema ("bearer"),
 *   Refresh Token, tiempo de expiración en segundos y alcance (scope).
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Forma parte de los contratos REST base de autenticación de usuarios.
 *   - [Actividad 11]: Contrato de entrega de tokens JWT 100% STATELESS firmados con
 *     clave simétrica HMAC-SHA aceptados indistintamente por Instancia A (:8080) e
 *     Instancia B (:8082).
 */

// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// [Actividad 1] [Actividad 11] Representa la respuesta JSON estándar OAuth 2.0 tras una autenticación exitosa.
public class TokenResponse {

    // [Actividad 11] Token JWT de acceso (tipo "access") enviado en Authorization: Bearer para rutas protegidas.
    private String access_token;
    // [Actividad 11] Esquema de autenticación OAuth 2.0 (valor "bearer").
    private String token_type;
    // [Actividad 11] Token JWT de renovación (tipo "refresh") con mayor tiempo de vigencia.
    private String refresh_token;
    // [Actividad 11] Tiempo de vida del Access Token expresado en segundos.
    private long expires_in;
    // [Actividad 11] Alcance lógico de permisos otorgado al token (ej. "vehiculos").
    private String scope;

    // Constructor sin argumentos requerido por Jackson y frameworks de serialización JSON.
    public TokenResponse() {
    }

    // [Actividad 11] Constructor completo invocado por AuthController para construir la respuesta de /oauth/token.
     public TokenResponse(String access_token,
            // Esquema de autenticación asociado al token (ej. "bearer").
            String token_type,
            // Token JWT de renovación emitido junto con el Access Token.
            String refresh_token,
            // Vigencia del Access Token expresada en segundos.
            long expires_in,
            // Alcance de autorización asignado al token.
            String scope) {
                // Asigna el Access Token recibido al atributo access_token.
                this.access_token=access_token;
                // Asigna el tipo de token recibido al atributo token_type.
                this.token_type = token_type;
                // Asigna el Refresh Token recibido al atributo refresh_token.
                this.refresh_token = refresh_token;
                // Asigna el tiempo de expiración en segundos al atributo expires_in.
                this.expires_in = expires_in;
                // Asigna el alcance recibido al atributo scope.
                this.scope = scope;
    }


    // Actualiza el Access Token almacenado en el DTO.
    public void setAccess_token(String access_token) {
        // Guarda el valor recibido en access_token.
        this.access_token = access_token;
    }

    // Actualiza el esquema de autenticación (token_type) del DTO.
    public void setToken_type(String token_type) {
        // Guarda el valor recibido en token_type.
        this.token_type = token_type;
    }

    // Actualiza el Refresh Token almacenado en el DTO.
    public void setRefresh_token(String refresh_token) {
        // Guarda el valor recibido en refresh_token.
        this.refresh_token = refresh_token;
    }

    // Actualiza la duración del Access Token en segundos.
    public void setExpires_in(long expires_in) {
        // Guarda el valor recibido en expires_in.
        this.expires_in = expires_in;
    }

    // Actualiza el alcance (scope) del DTO.
    public void setScope(String scope) {
        // Guarda el valor recibido en scope.
        this.scope = scope;
    }


    // Devuelve el Access Token JWT para serialización JSON como "access_token".
     public String getAccess_token() {
         // Retorna el contenido de access_token.
         return access_token;
     }

     // Devuelve el tipo de token para serialización JSON como "token_type".
     public String getToken_type() {
         // Retorna el contenido de token_type.
         return token_type;
     }

     // Devuelve el Refresh Token JWT para serialización JSON como "refresh_token".
     public String getRefresh_token() {
         // Retorna el contenido de refresh_token.
         return refresh_token;
     }

     // Devuelve la vigencia en segundos del Access Token para serialización JSON como "expires_in".
     public long getExpires_in() {
         // Retorna el contenido de expires_in.
         return expires_in;
     }

     // Devuelve el alcance de autorización para serialización JSON como "scope".
     public String getScope() {
         // Retorna el contenido de scope.
         return scope;
     }
    
}
