package ec.edu.uta.venta_vehiculos_api.service;

import java.math.BigDecimal;
import java.time.Year;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ec.edu.uta.venta_vehiculos_api.dto.*;
import ec.edu.uta.venta_vehiculos_api.exception.ApiError;
import ec.edu.uta.venta_vehiculos_api.model.Vehiculo;
import ec.edu.uta.venta_vehiculos_api.repository.*;

// Declara la clase como componente de servicio administrado por Spring
@Service
// Habilita el control transaccional automático para todas las operaciones de la clase
@Transactional
public class VehiculoService {

    // Repositorio para operaciones de persistencia de vehículos
    private final VehiculoRepository repo;

    // Repositorio para consultar el historial o estado de reservas asociadas
    private final ReservaRepository reservas;

    // Inyección de dependencias mediante constructor para los repositorios requeridos
    public VehiculoService(
            VehiculoRepository repo,
            ReservaRepository reservas) {
        this.repo = repo;
        this.reservas = reservas;
    }

    // Consulta de sólo lectura para listar vehículos con filtro opcional por marca
    @Transactional(readOnly = true)
    public List<VehiculoResponse> listar(String marca) {
        // Si no se envía marca se buscan todos, de lo contrario se filtra por coincidencia ignorando mayúsculas/minúsculas
        var lista = marca == null
                ? repo.findAll()
                : repo.findByMarcaContainingIgnoreCase(marca);

        // Transforma la lista de entidades Vehiculo al DTO VehiculoResponse
        return lista.stream()
                .map(VehiculoResponse::de)
                .toList();
    }

    // Consulta de sólo lectura para obtener un vehículo por su identificador primario
    @Transactional(readOnly = true)
    public VehiculoResponse obtener(int id) {
        // Busca por ID; si no existe lanza excepción ApiError 404
        return VehiculoResponse.de(
                repo.findById(id)
                    .orElseThrow(() -> falta())
        );
    }

    // Registra un nuevo vehículo con estado inicial DISPONIBLE
    public VehiculoResponse crear(VehiculoRequest d) {
        Vehiculo v = new Vehiculo();
        // Copia y valida los campos recibidos desde el DTO hacia la entidad
        copiar(v, d);
        // Asigna el estado inicial obligatorio para vehículos recién creados
        v.setEstado("DISPONIBLE");

        // Guarda y fuerza la sincronización con la base de datos (saveAndFlush)
        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // Actualiza la totalidad de los datos de un vehículo editable
    public VehiculoResponse actualizar(int id, VehiculoRequest d) {
        // Bloquea y verifica que el vehículo esté disponible para modificación
        Vehiculo v = editable(id);
        // Actualiza los valores de los atributos con las validaciones correspondientes
        copiar(v, d);

        // Guarda cambios inmediatamente en base de datos
        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // Actualización parcial (PATCH) exclusiva para modificar el precio del vehículo
    public VehiculoResponse precio(int id, Map<String, Object> d) {
        // Valida que el mapa contenga únicamente la clave "precio" y que sea numérico
        if (d.size() != 1 ||
                !d.containsKey("precio") ||
                !(d.get("precio") instanceof Number)) {

            throw new ApiError(
                    422,
                    "PATCH_INVALIDO",
                    "PATCH admite únicamente precio numérico y no nulo"
            );
        }

        BigDecimal precio;
        try {
            // Convierte el valor a BigDecimal para precisión monetaria
            precio = new BigDecimal(d.get("precio").toString());
        } catch (NumberFormatException e) {
            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio inválido"
            );
        }

        // Valida que el precio sea mayor que cero, máximo 2 decimales y menor al tope permitido
        if (precio.signum() <= 0 ||
                precio.scale() > 2 ||
                precio.compareTo(
                    new BigDecimal("9999999999.99")) > 0) {

            throw new ApiError(
                    422,
                    "PRECIO_INVALIDO",
                    "Precio positivo con máximo dos decimales"
            );
        }

        // Obtiene la entidad bloqueada y en estado editable
        Vehiculo v = editable(id);
        // Aplica el nuevo precio y persiste
        v.setPrecio(precio);

        return VehiculoResponse.de(repo.saveAndFlush(v));
    }

    // Elimina un vehículo verificando previamente que no tenga reservas históricas
    public void eliminar(int id) {
        // Bloquea y verifica estado DISPONIBLE
        Vehiculo v = editable(id);

        // Regla de integridad: si el vehículo tiene historial de reservas no puede eliminarse
        if (reservas.existsByVehiculo_IdVehiculo(id)) {
            throw new ApiError(
                    409,
                    "HISTORIAL_RESERVAS",
                    "Se conserva el vehículo por su historial de reservas"
            );
        }

        // Elimina el registro y sincroniza de inmediato
        repo.delete(v);
        repo.flush();
    }

    // Método auxiliar para bloquear concurrentemente y asegurar que el vehículo esté DISPONIBLE
    private Vehiculo editable(int id) {
        // Aplica bloqueo pesimista mediante la consulta JPQL personalizada
        Vehiculo v = repo.bloquear(id)
                .orElseThrow(() -> falta());

        // Si el vehículo ya fue reservado o vendido se rechaza cualquier modificación o borrado
        if (!"DISPONIBLE".equals(v.getEstado())) {
            throw new ApiError(
                    409,
                    "VEHICULO_NO_DISPONIBLE",
                    "Un vehículo reservado o vendido no se modifica ni elimina"
            );
        }

        return v;
    }

    // Construye la excepción estándar para recursos inexistentes (404)
    private ApiError falta() {
        return new ApiError(
                404,
                "VEHICULO_NO_ENCONTRADO",
                "El vehículo no existe"
        );
    }

    // Copia y valida los campos desde el DTO hacia la entidad Vehiculo
    private void copiar(Vehiculo v, VehiculoRequest d) {
        // Validación del rango de año
        if (d.anio() < 1900 ||
                d.anio() > Year.now().getValue() + 1) {

            throw new ApiError(
                    422,
                    "ANIO_INVALIDO",
                    "Año fuera de rango"
            );
        }

        // Limpieza y asignación de marca y modelo
        v.setMarca(d.marca().trim());
        v.setModelo(d.modelo().trim());

        // Normalización a mayúsculas de placa y chasis
        v.setPlaca(
                d.placa().trim().toUpperCase(Locale.ROOT)
        );
        v.setChasis(
                d.chasis().trim().toUpperCase(Locale.ROOT)
        );

        // Validación estricta de longitud del chasis (exactamente 17 caracteres)
        if (v.getChasis().length() != 17) {
            throw new ApiError(
                    422,
                    "CHASIS_INVALIDO",
                    "Chasis de 17 caracteres"
            );
        }

        // Asignación de atributos restantes
        v.setAnio(d.anio());
        v.setColor(d.color());
        v.setPrecio(d.precio());
    }

}