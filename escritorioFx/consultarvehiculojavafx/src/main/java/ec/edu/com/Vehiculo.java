package ec.edu.com;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties (ignoreUnknown = true)

public record Vehiculo(
    int idVehiculo, 
    String marca, 
    String modelo,
    String placa,
    String chasis,
    int año, 
    String color,
    BigDecimal precio,
    String estado) {
    
}