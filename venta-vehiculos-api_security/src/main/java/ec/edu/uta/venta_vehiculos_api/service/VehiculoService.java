/**
 * Archivo: VehiculoService.java
 * Responsabilidad arquitectónica:
 *   Capa de servicio transaccional que gestiona el catálogo e inventario de vehículos
 *   (listar con filtro por marca, consultar por ID, crear en estado DISPONIBLE,
 *   actualizar integralmente, modificar parcialmente solo el precio vía PATCH y
 *   eliminar preservando la integridad referencial con reservas históricas).
 *   Aplica validaciones de dominio, normalización de placa/chasis y bloqueo pesimista
 *   nativo FOR UPDATE antes de cualquier modificación o eliminación.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Implementa toda la lógica CRUD, validaciones de negocio (422),
 *     búsquedas (404) y protección de conflictos (409) del contrato REST de vehículos.
 *   - [Actividad 4]: Protege la máquina de estados DISPONIBLE -> RESERVADO -> VENDIDO
 *     impidiendo modificar o eliminar un vehículo que se encuentre RESERVADO o VENDIDO
 *     mediante el mismo bloqueo pesimista nativo SQL FOR UPDATE compatible con
 *     MariaDB 10.4 que usa ReservaService.
 *   - [Actividad 10 - P27]: Garantiza coherencia transversal del inventario entre
 *     Instancia A (:8080) e Instancia B (:8082) sincronizando cambios inmediatos con
 *     saveAndFlush/flush y serializando escrituras concurrentes con bloqueo de fila.
 */

// Declara el paquete que concentra los servicios de negocio del microservicio.
package ec.edu.uta.venta_vehiculos_api.service;

// Importa BigDecimal para validar y almacenar precios monetarios exactos.
import java.math.BigDecimal;
// Importa Year para validar dinámicamente el límite superior del año de fabricación.
import java.time.Year;
// Importa colecciones y Locale.ROOT para normalización determinista de cadenas a mayúsculas.
import java.util.*;

// Importa @Service para registrar la clase como bean de servicio Spring.
import org.springframework.stereotype.Service;
// Importa @Transactional para delimitar transacciones ACID por operación de servicio.
import org.springframework.transaction.annotation.Transactional;

// Importa los DTOs VehiculoRequest y VehiculoResponse.
import ec.edu.uta.venta_vehiculos_api.dto.*;
// Importa ApiError para señalar errores controlados 404, 409 y 422 hacia ApiExceptionHandler.
import ec.edu.uta.venta_vehiculos_api.exception.ApiError;
// Importa la entidad JPA Vehiculo.
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;
// Importa VehiculoRepository y ReservaRepository.
import ec.edu.uta.venta_vehiculos_api.repository.*;

// [Actividad 1] [Actividad 10 - P27] Registra el servicio de gestión del inventario de vehículos.
@Service
// Ejecuta cada método público dentro de una transacción relacional gestionada por Spring.
@Transactional
// Servicio que aplica reglas de negocio, validaciones y control concurrente sobre vehículos.
public class VehiculoService {

    // Repositorio JPA de vehículos (incluye bloqueo pesimista nativo SQL FOR UPDATE).
    private final VehiculoRepository repo;

    // Repositorio JPA de reservas consultado para impedir la eliminación de vehículos con historial.
    private final ReservaRepository reservas;

    // Constructor con inyección de dependencias de los repositorios de vehículos y reservas.
    public VehiculoService(
            // Repositorio principal de persistencia de vehículos.
            VehiculoRepository repo,
            // Repositorio auxiliar para verificar integridad histórica de reservas.
            ReservaRepository reservas) {
        // Almacena el repositorio de vehículos.
        this.repo = repo;
        // Almacena el repositorio de reservas.
        this.reservas = reservas;
    }

    // [Actividad 1] [Actividad 10 - P27] Lista todos los vehículos o filtra por subcadena de marca en transacción de solo lectura.
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(String marca) {
        // Selecciona consulta completa o búsqueda insensible a mayúsculas según si se suministró el filtro marca.
        var lista = marca == null
                ? repo.findAll()
                : repo.findByMarcaContainingIgnoreCase(marca);

        // Proyecta cada entidad Vehiculo al DTO inmutable VehiculoResponse.
        return lista.stream()
                // Convierte cada entidad mediante su método estático de fábrica.
                .map(VehiculoResponse::de)
                // Recolecta la lista inmutable de respuestas.
                .toList();
    }

    // [Actividad 1] [Actividad 10 - P27] Obtiene un vehículo específico por ID en transacción de solo lectura o lanza 404.
    @Transactional(readOnly = true)
    public VehiculoResponse obtener(int id) {
        // Busca la entidad por clave primaria y la mapea a VehiculoResponse (o lanza ApiError 404).
        return VehiculoResponse.de(
                repo.findById(id)
                    .orElseThrow(() -> falta())
        );
    }

    // [Actividad 1] [Actividad 4] [Actividad 10 - P27] Da de alta un vehículo nuevo con estado inicial obligatorio DISPONIBLE.
    public VehiculoResponse crear(VehiculoRequest d) {
        // Crea una instancia transitoria de la entidad Vehiculo.
        Vehiculo v = new Vehiculo();
        // Valida rangos, normaliza textos y copia los atributos del DTO hacia la entidad.
        copiar(v, d);
        // [Actividad 4] Fija el estado inicial DISPONIBLE al iniciar su ciclo de vida en el inventario.
        v.setEstado("DISPONIBLE");

        // Persiste y ejecuta flush inmediato para detectar duplicados de placa/chasis en MariaDB antes de responder.
        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // [Actividad 1] [Actividad 4] [Actividad 10 - P27] Actualiza integralmente un vehículo verificando bloqueo FOR UPDATE y estado DISPONIBLE.
    public VehiculoResponse actualizar(int id, VehiculoRequest d) {
        // Adquiere bloqueo pesimista FOR UPDATE y confirma que el vehículo siga DISPONIBLE (404 o 409 si no).
        Vehiculo v = editable(id);
        // Revalida y sobreescribe todos los atributos editables desde el DTO recibido.
        copiar(v, d);

        // Guarda y sincroniza de inmediato con MariaDB devolviendo el DTO actualizado.
        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // [Actividad 1] [Actividad 4] [Actividad 10 - P27] Aplica PATCH parcial exclusivo sobre el campo "precio" bajo bloqueo pesimista.
    public VehiculoResponse precio(int id, Map<String, Object> d) {
        // Verifica contrato estricto: el cuerpo merge-patch debe tener exactamente 1 clave llamada "precio" con valor numérico.
        if (d.size() != 1 ||
                !d.containsKey("precio") ||
                !(d.get("precio") instanceof Number)) {

            // Rechaza con HTTP 422 Unprocessable Entity cualquier intento de modificar otros campos vía PATCH.
            throw new ApiError(
                    422,
                    "PATCH_INVALIDO",
                    "PATCH admite únicamente precio numérico y no nulo"
            );
        }

        // Declara la variable BigDecimal que almacenará el nuevo precio validado.
        BigDecimal precio;
        // Convierte la representación textual del número a BigDecimal para evitar imprecisiones binarias.
        try {
            // Construye BigDecimal a partir de toString() del valor numérico recibido.
            precio = new BigDecimal(d.get("precio").toString());
        // Captura formatos numéricos inválidos (ej. NaN o Infinity).
        } catch (NumberFormatException e) {
            // Traduce el error de parseo a HTTP 422 PRECIO_INVALIDO.
            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio inválido"
            );
        }

        // Valida que el precio sea estrictamente positivo (> 0), tenga máximo 2 decimales y quepa en DECIMAL(12,2).
        if (precio.signum() <= 0 ||
                precio.scale() > 2 ||
                precio.compareTo(
                    new BigDecimal("9999999999.99")) > 0) {

            // Rechaza con HTTP 422 si viola las restricciones financieras de escala o rango.
            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio positivo con máximo dos decimales"
            );
        }

        // [Actividad 4] [Actividad 10 - P27] Adquiere bloqueo pesimista FOR UPDATE y verifica estado DISPONIBLE antes de cambiar el precio.
        Vehiculo v = editable(id);
        // Actualiza exclusivamente el atributo precio en la entidad bloqueada.
        v.setPrecio(precio);

        // Persiste y hace flush inmediato devolviendo la representación DTO actualizada.
        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // [Actividad 1] [Actividad 4] [Actividad 10 - P27] Elimina físicamente un vehículo solo si está DISPONIBLE y carece de historial de reservas.
    public void eliminar(int id) {
        // Adquiere bloqueo pesimista FOR UPDATE y exige que el vehículo esté en estado DISPONIBLE.
        Vehiculo v = editable(id);

        // [Actividad 4] [Actividad 10 - P27] Preserva trazabilidad e integridad: bloquea borrado si existen registros históricos en tabla reservas.
        if (reservas.existsByVehiculo_IdVehiculo(id)) {
            // Lanza HTTP 409 Conflict evitando romper la auditoría y llaves foráneas de reservas.
            throw new ApiError(
                    409,
                    "HISTORIAL_RESERVAS",
                    "Se conserva el vehículo por su historial de reservas"
            );
        }

        // Marca la entidad para eliminación y ejecuta flush inmediato contra MariaDB.
        repo.delete(v);
        // Fuerza la ejecución SQL DELETE dentro de la transacción activa.
        repo.flush();
    }

    // [Actividad 4] [Actividad 10 - P27] Adquiere bloqueo pesimista nativo SQL FOR UPDATE (MariaDB 10.4) y verifica que el vehículo siga DISPONIBLE.
    private Vehiculo editable(int id) {
        // Ejecuta SELECT * FROM vehiculos WHERE id_vehiculo = :id FOR UPDATE; lanza 404 si no existe.
        Vehiculo v = repo.bloquear(id)
                .orElseThrow(() -> falta());

        // [Actividad 4] Impide modificar o eliminar vehículos en estado RESERVADO o VENDIDO para proteger ventas en curso o concluidas.
        if (!"DISPONIBLE".equals(v.getEstado())) {
            // Lanza HTTP 409 Conflict indicando restricción por estado de negocio.
            throw new ApiError(
                    409,
                    "VEHICULO_NO_DISPONIBLE",
                    "Un vehículo reservado o vendido no se modifica ni elimina"
            );
        }

        // Retorna la entidad bloqueada y apta para edición o eliminación.
        return v;
    }

    // Fábrica auxiliar que construye el ApiError HTTP 404 VEHICULO_NO_ENCONTRADO.
    private ApiError falta() {
        // Devuelve la excepción de recurso inexistente traducida a RFC 7807 por ApiExceptionHandler.
        return new ApiError(
                404,
                "VEHICULO_NO_ENCONTRADO",
                "El vehículo no existe"
        );
    }

    // [Actividad 1] Valida reglas de dominio (año y longitud real de chasis) y normaliza los campos del DTO en la entidad Vehiculo.
    private void copiar(Vehiculo v, VehiculoRequest d) {
        // Verifica que el año de fabricación esté entre 1900 y el año calendario siguiente al actual.
        if (d.anio() < 1900 ||
                d.anio() > Year.now().getValue() + 1) {

            // Lanza HTTP 422 Unprocessable Entity si el año está fuera del rango válido.
            throw new ApiError(
                    422,
                    "ANIO_INVALIDO",
                    "Año fuera de rango"
            );
        }

        // Elimina espacios sobrantes en extremos de marca y modelo antes de persistir.
        v.setMarca(d.marca().trim());
        // Asigna el modelo recortado.
        v.setModelo(d.modelo().trim());

        // Normaliza placa a mayúsculas independientes de configuración regional (Locale.ROOT) para unicidad consistente.
        v.setPlaca(
                d.placa().trim().toUpperCase(Locale.ROOT)
        );
        // Normaliza el número de chasis (VIN) a mayúsculas sin espacios en extremos.
        v.setChasis(
                d.chasis().trim().toUpperCase(Locale.ROOT)
        );

        // Revalida tras trim() que el código VIN de chasis mantenga exactamente 17 caracteres efectivos.
        if (v.getChasis().length() != 17) {
            // Lanza HTTP 422 si al eliminar espacios el chasis dejó de tener 17 caracteres.
            throw new ApiError(
                    422,
                    "CHASIS_INVALIDO",
                    "Chasis de 17 caracteres"
            );
        }

        // Asigna año, color y precio validados hacia la entidad persistente.
        v.setAnio(d.anio());
        // Asigna el color opcional recibido.
        v.setColor(d.color());
        // Asigna el precio decimal validado.
        v.setPrecio(d.precio());
    }

}
