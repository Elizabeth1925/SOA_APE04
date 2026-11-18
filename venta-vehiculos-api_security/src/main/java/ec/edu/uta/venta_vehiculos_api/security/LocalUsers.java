package ec.edu.uta.venta_vehiculos_api.security;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import ec.edu.uta.venta_vehiculos_api.repository.UsuarioApiRepository;
import ec.edu.uta.venta_vehiculos_api.model.UsuarioApi;

// Configuración opcional activa exclusivamente bajo el perfil "local"
@Configuration
@Profile("local")
public class LocalUsers {

    // Ejecuta una tarea de inicialización al arrancar la aplicación para crear un usuario de prueba si no existe
    @Bean 
    CommandLineRunner usuarioInicial(
            UsuarioApiRepository repo,
            PasswordEncoder encoder,
            @Value("${app.bootstrap.user}") String user,
            @Value("${app.bootstrap.password}") String password,
            @Value("${app.bootstrap.role:ADMIN}") String rol) {

        return args -> {
            // Valida que el rol configurado pertenezca a los roles admitidos
            if (!java.util.List.of("ADMIN", "OPERADOR", "CONSULTA").contains(rol)) {
                throw new IllegalArgumentException("Rol inicial inválido");
            }

            // Comprueba si el usuario ya existe en base de datos; si no, lo registra con contraseña hasheada
            if (repo.findByUsername(user).isEmpty()) {
                UsuarioApi u = new UsuarioApi(); 
                u.setUsername(user);
                u.setPassword(encoder.encode(password)); 
                u.setRol(rol);

                repo.save(u);
            }
        };
    }
}
