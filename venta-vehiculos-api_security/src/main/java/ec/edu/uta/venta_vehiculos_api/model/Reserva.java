/**
 * Archivo: Reserva.java
 * Responsabilidad arquitectónica:
 *   Entidad JPA mapeada a la tabla "reservas" en el esquema relacional compartido
 *   venta_vehiculos. Persiste el estado y trazabilidad de cada reserva creada durante
 *   el protocolo de venta en 2 fases, garantizando idempotencia a nivel de base de
 *   datos mediante la restricción UNIQUE sobre operacion_id.
 * Actividades APE 04 en las que participa:
 *   - [Actividad 4]: Modela la entidad central de la orquestación en 2 fases con
 *     ventas-api (FastAPI), registrando transiciones RESERVADA -> CONFIRMADA o
 *     RESERVADA -> LIBERADA vinculadas al vehículo reservado.
 *   - [Actividad 10 - P27]: Al persistirse en la tabla compartida entre Instancia A
 *     (:8080) e Instancia B (:8082), garantiza coherencia transversal del estado de
 *     reservas ante balanceo de carga 3:1 en NGINX.
 */

// Declara el paquete que aloja las entidades persistentes del dominio.
package ec.edu.uta.venta_vehiculos_api.model;

// Importa BigDecimal para almacenar el precio congelado de la reserva con precisión exacta.
import java.math.BigDecimal;
// Importa las anotaciones estándar de Jakarta Persistence (JPA).
import jakarta.persistence.*;

// [Actividad 4] Marca la clase como entidad relacional administrada por JPA/Hibernate.
@Entity
// [Actividad 4] [Actividad 10 - P27] Vincula la entidad con la tabla física "reservas" en MariaDB.
@Table(name="reservas")
// Entidad persistente que representa una reserva de compra asociada a un vehículo.
public class Reserva {

    // Clave primaria de la reserva (UUID generado en ReservaService).
    @Id
    // Mapea la columna id con longitud fija máxima de 36 caracteres para UUID estándar.
    @Column(length=36)
    // Identificador único de la reserva expuesto a ventas-api.
    public String id;

    // [Actividad 4] [Actividad 10 - P27] Restricción UNIQUE y NOT NULL que asegura idempotencia a nivel de BD ante reintentos.
    @Column(name="operacion_id", nullable=false, unique=true, length=64)
    // Identificador único de operación enviado por ventas-api para correlacionar la venta.
    public String operacionId;

    // [Actividad 4] Relación muchos-a-uno obligatoria con carga diferida (LAZY) hacia el vehículo reservado.
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    // Define la columna de llave foránea vehiculo_id que referencia a vehiculos.id_vehiculo.
    @JoinColumn(name="vehiculo_id", nullable=false)
    // Entidad Vehiculo cuyo estado transita junto con esta reserva (DISPONIBLE -> RESERVADO -> VENDIDO).
    public Vehiculo vehiculo;

    // Columna monetaria obligatoria con 12 dígitos totales y 2 decimales (precio congelado al reservar).
    @Column(nullable=false, precision=12, scale=2)
    // Monto pactado del vehículo al momento de crear la reserva.
    public BigDecimal precio;

    // Columna obligatoria de 3 caracteres para el código ISO de moneda.
    @Column(nullable=false, length=3)
    // Moneda de la operación (por ejemplo, "USD").
    public String moneda;

    // [Actividad 4] Columna obligatoria que registra el estado de la reserva: RESERVADA, CONFIRMADA o LIBERADA.
    @Column(nullable=false, length=20)
    // Estado vigente de la reserva dentro de la orquestación en 2 fases.
    public String estado;

    // Constructor sin parámetros exigido por la especificación JPA para instanciación por reflexión.
    public Reserva() {}
}
