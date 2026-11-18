package ec.edu.uta.venta_vehiculos_api.model;

import java.math.BigDecimal;
import jakarta.persistence.*;

// Entidad JPA mapeada a la tabla "reservas" en la base de datos
@Entity
@Table(name="reservas")
public class Reserva {

    // Identificador único de la reserva (UUID en formato texto de 36 caracteres)
    @Id
    @Column(length=36)
    public String id;

    // Identificador único de la operación de negocio / idempotencia (hasta 64 caracteres)
    @Column(name="operacion_id", nullable=false, unique=true, length=64)
    public String operacionId;

    // Relación Many-to-One con el vehículo asociado (carga perezosa LAZY y obligatoria)
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="vehiculo_id", nullable=false)
    public Vehiculo vehiculo;

    // Precio pactado al momento de la reserva con precisión monetaria
    @Column(nullable=false, precision=12, scale=2)
    public BigDecimal precio;

    // Código ISO de la moneda (por ejemplo, "USD")
    @Column(nullable=false, length=3)
    public String moneda;

    // Estado del ciclo de vida de la reserva: RESERVADA, CONFIRMADA, LIBERADA
    @Column(nullable=false, length=20)
    public String estado;

    // Constructor por defecto requerido por JPA
    public Reserva() {}
}
