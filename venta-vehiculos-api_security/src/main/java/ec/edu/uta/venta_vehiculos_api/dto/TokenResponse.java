// Identifica el paquete que contiene los objetos de transferencia de datos.
package ec.edu.uta.venta_vehiculos_api.dto;

// Representa los datos JSON que se devuelven al cliente después de autenticarse.
public class TokenResponse {

    // Almacena el token de acceso utilizado para autorizar peticiones protegidas.
    private String access_token;
    // Almacena el esquema de autenticación que debe acompañar al token.
    private String token_type;
    // Almacena el token que permite solicitar una renovación del acceso.
    private String refresh_token;
    // Almacena la duración del token de acceso, expresada en segundos.
    private long expires_in;
    // Almacena el alcance o conjunto de permisos asociado al token.
    private String scope;

    // Constructor vacío disponible para bibliotecas de serialización y frameworks.
    public TokenResponse() {
    }

    // Construye una respuesta de token con todos los valores que se devolverán al cliente.
     public TokenResponse(String access_token,
            // Token de acceso emitido para el usuario.
            String token_type,
            // Tipo de esquema de autenticación del token.
            String refresh_token,
            // Token para renovar el acceso cuando corresponda.
            long expires_in,
            // Alcance de autorización incluido en la respuesta.
            String scope) {
                // Asigna el token de acceso al campo correspondiente.
                this.access_token=access_token;
                // Asigna el tipo de token al campo correspondiente.
                this.token_type = token_type;
                // Asigna el token de renovación al campo correspondiente.
                this.refresh_token = refresh_token;
                // Asigna la duración del token al campo correspondiente.
                this.expires_in = expires_in;
                // Asigna el alcance de autorización al campo correspondiente.
                this.scope = scope;
    }


    // Reemplaza el token de acceso almacenado.
    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    // Reemplaza el tipo de token almacenado.
    public void setToken_type(String token_type) {
        this.token_type = token_type;
    }

    // Reemplaza el token de renovación almacenado.
    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    // Reemplaza la duración del token expresada en segundos.
    public void setExpires_in(long expires_in) {
        this.expires_in = expires_in;
    }

    // Reemplaza el alcance de autorización almacenado.
    public void setScope(String scope) {
        this.scope = scope;
    }


    // Devuelve el token de acceso.
     public String getAccess_token() {
         return access_token;
     }

     // Devuelve el esquema de autenticación del token.
     public String getToken_type() {
         return token_type;
     }

     // Devuelve el token de renovación.
     public String getRefresh_token() {
         return refresh_token;
     }

     // Devuelve cuánto tiempo es válido el token en segundos.
     public long getExpires_in() {
         return expires_in;
     }

     // Devuelve el alcance de autorización del token.
     public String getScope() {
         return scope;
     }
    
}
