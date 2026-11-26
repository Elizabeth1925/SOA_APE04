/**
 * Archivo: Vehiculo.java
 * Responsabilidad arquitectónica:
 *   Entidad JPA mapeada a la tabla "vehiculos" en el esquema relacional compartido
 *   venta_vehiculos. Modela los atributos del inventario de vehículos y su estado
 *   de ciclo de vida (DISPONIBLE -> RESERVADO -> VENDIDO) sujeto a bloqueo pesimista
 *   nativo FOR UPDATE durante operaciones concurrentes.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 1]: Modelo JPA principal del catálogo REST de vehículos.
 *   - [Actividad 4]: Soporta la máquina de estados DISPONIBLE -> RESERVADO -> VENDIDO
 *     (y reversión RESERVADO -> DISPONIBLE) coordinada por ReservaService con ventas-api.
 *   - [Actividad 10 - P27]: Al persistirse en la tabla compartida de MariaDB, asegura
 *     coherencia transversal del inventario entre Instancia A (:8080) e Instancia B (:8082).
 */

// Define el paquete dentro del proyecto donde se ubican las entidades del modelo.
package ec.edu.uta.venta_vehiculos_api.model;

// Importa BigDecimal para almacenar precios monetarios con precisión decimal exacta.
import java.math.BigDecimal;

// Importa @Column para especificar nombres, precisión y nulabilidad de columnas.
import jakarta.persistence.Column;
// Importa @Entity para declarar esta clase como entidad persistente JPA.
import jakarta.persistence.Entity;
// Importa @GeneratedValue para delegar la generación de la clave primaria.
import jakarta.persistence.GeneratedValue;
// Importa GenerationType para indicar la estrategia IDENTITY.
import jakarta.persistence.GenerationType;
// Importa @Id para marcar el atributo que funge como clave primaria.
import jakarta.persistence.Id;
// Importa @Table para mapear la entidad a la tabla física "vehiculos".
import jakarta.persistence.Table;

// [Actividad 1] Declara la clase como entidad relacional administrada por JPA/Hibernate.
@Entity
// [Actividad 1] [Actividad 10 - P27] Vincula la entidad con la tabla física "vehiculos" en MariaDB.
@Table(name = "vehiculos")
// Entidad persistente que representa un vehículo dentro del inventario del concesionario.
public class Vehiculo {

    // Marca idVehiculo como clave primaria de la tabla vehiculos.
    @Id
    // Usa generación autoincremental nativa (AUTO_INCREMENT) de MariaDB/MySQL.
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // Mapea explícitamente el atributo Java camelCase a la columna SQL snake_case "id_vehiculo".
    @Column(name = "id_vehiculo")
    // Identificador numérico único del vehículo.
    private Integer idVehiculo;

    // Marca comercial del vehículo (ej. Toyota, Chevrolet).
    private String marca;

    // Modelo comercial del vehículo (ej. Corolla, Sail).
    private String modelo;

    // Placa o matrícula única del vehículo, normalizada en mayúsculas por VehiculoService.
    private String placa;

    // Número de chasis (VIN) único de 17 caracteres normalizado en mayúsculas.
    private String chasis;

    // Año de fabricación del vehículo validado en rango permitido.
    private Integer anio;

    // Color exterior del vehículo (campo opcional).
    private String color;

    // Restricción de columna SQL DECIMAL(12,2) NOT NULL para exactitud financiera del precio.
    @Column (precision = 12, scale = 2, nullable = false) // Define la precisión y escala para el precio 
    // Precio de venta del vehículo en moneda base.
    private BigDecimal precio;

    // [Actividad 4] [Actividad 10 - P27] Estado transaccional del vehículo: DISPONIBLE, RESERVADO o VENDIDO.
    private String estado;

    // Constructor vacío requerido por el estándar JPA para la creación de proxies y entidades.
    public Vehiculo() {
    }

    // Devuelve la clave primaria del vehículo.
    public Integer getIdVehiculo() {
        // Retorna el valor del atributo idVehiculo.
        return idVehiculo;
    }

    // Establece o actualiza la clave primaria del vehículo.
    public void setIdVehiculo(Integer idVehiculo) {
        // Guarda el valor recibido en idVehiculo.
        this.idVehiculo = idVehiculo;
    }

    // Devuelve la marca comercial del vehículo.
    public String getMarca() {
        // Retorna el valor del atributo marca.
        return marca;
    }

    // Establece o actualiza la marca comercial del vehículo.
    public void setMarca(String marca) {
        // Guarda el valor recibido en marca.
        this.marca = marca;
    }

    // Devuelve el modelo comercial del vehículo.
    public String getModelo() {
        // Retorna el valor del atributo modelo.
        return modelo;
    }

    // Establece o actualiza el modelo comercial del vehículo.
    public void setModelo(String modelo) {
        // Guarda el valor recibido en modelo.
        this.modelo = modelo;
    }

    // Devuelve la placa o matrícula del vehículo.
    public String getPlaca() {
        // Retorna el valor del atributo placa.
        return placa;
    }

    // Establece o actualiza la placa del vehículo.
    public void setPlaca(String placa) {
        // Guarda el valor recibido en placa.
        this.placa = placa;
    }

    // Devuelve el código de chasis (VIN) de 17 caracteres.
    public String getChasis() {
        // Retorna el valor del atributo chasis.
        return chasis;
    }

    // Establece o actualiza el código de chasis (VIN) del vehículo.
    public void setChasis(String chasis) {
        // Guarda el valor recibido en chasis.
        this.chasis = chasis;
    }

    // Devuelve el año de fabricación del vehículo.
    public Integer getAnio() {
        // Retorna el valor del atributo anio.
        return anio;
    }

    // Establece o actualiza el año de fabricación del vehículo.
    public void setAnio(Integer anio) {
        // Guarda el valor recibido en anio.
        this.anio = anio;
    }

    // Devuelve el color registrado del vehículo.
    public String getColor() {
        // Retorna el valor del atributo color.
        return color;
    }

    // Establece o actualiza el color del vehículo.
    public void setColor(String color) {
        // Guarda el valor recibido en color.
        this.color = color;
    }

    // Devuelve el precio decimal actual del vehículo.
    public BigDecimal getPrecio() {
        // Retorna el valor del atributo precio.
        return precio;
    }

    // Establece o actualiza el precio decimal del vehículo.
    public void setPrecio(BigDecimal precio) {
        // Guarda el valor recibido en precio.
        this.precio = precio;
    }

    // [Actividad 4] Devuelve el estado vigente del vehículo (DISPONIBLE, RESERVADO o VENDIDO).
    public String getEstado() {
        // Retorna el valor del atributo estado.
        return estado;
    }

    // [Actividad 4] Actualiza el estado del vehículo dentro de las transiciones válidas del negocio.
    public void setEstado(String estado) {
        // Guarda el valor recibido en estado.
        this.estado = estado;
    }
}
