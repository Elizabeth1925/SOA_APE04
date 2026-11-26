/**
 * ==============================================================================
 * ARCHIVO: MonitorService.java
 * COMPONENTE: monitor-balancer-service (Servicio de Sondeo Periodico y Alertas)
 * ROL: Ejecuta cada 5 segundos el sondeo activo de /nginx-ping (:8090) y
 *      /actuator/health (:8080 y :8082), calcula el estado agregado del sistema
 *      (NORMAL, DEGRADADO, SIN_BACKENDS, NGINX_DOWN), detecta transiciones de
 *      estado sin duplicados consecutivos y mantiene un historial acotado de
 *      las ultimas 20 alertas en memoria.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3]: Motor de observabilidad desacoplado con @Scheduled cada 5 s
 *     y lectura concurrente libre de bloqueos mediante AtomicReference.
 *   - [Actividad 11 - P23]: Valida NGINX=UP y ambas instancias UP => NORMAL.
 *   - [Actividad 11 - P25]: Detecta caida de una instancia (UP -> DOWN), pasa
 *     estadoGeneral a DEGRADADO y registra alertas auditables en historial.
 *   - [Actividad 11 - P26]: Detecta recuperacion (DOWN -> UP), restablece
 *     estadoGeneral a NORMAL y registra la alerta de recuperacion.
 * ==============================================================================
 */
// Declara el paquete de servicios de negocio de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.service;

// Importa OffsetDateTime para generar marcas temporales precisas con zona horaria local.
import java.time.OffsetDateTime;
// Importa DateTimeFormatter para serializar fechas en formato estandar ISO-8601.
import java.time.format.DateTimeFormatter;
// Importa ArrayList para devolver copias defensivas de las alertas al controlador.
import java.util.ArrayList;
// Importa Collections para crear listas sincronizadas y listas vacias inmutables.
import java.util.Collections;
// Importa LinkedList como estructura base para inserciones rapidas al inicio y poda al final.
import java.util.LinkedList;
// Importa la interfaz List para modelar colecciones de instancias y alertas.
import java.util.List;
// Importa AtomicReference para publicar atomicamente la ultima instantanea de salud sin bloquear lectores HTTP.
import java.util.concurrent.atomic.AtomicReference;

// Importa la interfaz Logger de SLF4J para trazas periodicas y advertencias de alertas.
import org.slf4j.Logger;
// Importa la fabrica de loggers SLF4J.
import org.slf4j.LoggerFactory;
// Importa @Value para inyectar URLs y puertos de los destinos monitoreados desde application.properties.
import org.springframework.beans.factory.annotation.Value;
// Importa @Scheduled para ejecutar automaticamente el sondeo cada 5000 ms.
import org.springframework.scheduling.annotation.Scheduled;
// Importa @Service para registrar el servicio singleton en Spring.
import org.springframework.stereotype.Service;

// Importa el cliente HTTP de sondeo con validacion explicita de JSON {"status":"UP"}.
import ec.edu.uta.monitor_balancer_service.client.HealthCheckClient;
// Importa el DTO de alerta de transicion.
import ec.edu.uta.monitor_balancer_service.dto.AlertaDto;
// Importa el DTO del estado agregado general.
import ec.edu.uta.monitor_balancer_service.dto.EstadoGeneralResponse;
// Importa el DTO de informacion de cada instancia backend.
import ec.edu.uta.monitor_balancer_service.dto.InstanciaInfo;

/**
 * Servicio central de monitoreo en tiempo real del balanceador NGINX y las
 * instancias de la API de Vehiculos.
 */
// Registra esta clase como servicio singleton administrado por el contenedor Spring.
@Service
public class MonitorService {

    // Logger estatico para registrar cada ciclo de 5s y cada alerta de transicion generada.
    private static final Logger log = LoggerFactory.getLogger(MonitorService.class);
    // Limite maximo de alertas retenidas en memoria para evitar crecimiento indefinido.
    private static final int MAX_ALERTAS = 20;

    // Cliente HTTP inyectado responsable de ejecutar cada consulta GET con timeouts finitos.
    private final HealthCheckClient client;

    // URL configurada del endpoint /nginx-ping en NGINX (:8090).
    @Value("${monitor.destinos.nginx.url}")
    private String nginxUrl;

    // URL configurada del endpoint /actuator/health de la Instancia A (:8080).
    @Value("${monitor.destinos.instancia-a.url}")
    private String instanciaAUrl;

    // Puerto numerico de la Instancia A (por defecto 8080).
    @Value("${monitor.destinos.instancia-a.puerto:8080}")
    private int instanciaAPuerto;

    // URL configurada del endpoint /actuator/health de la Instancia B (:8082).
    @Value("${monitor.destinos.instancia-b.url}")
    private String instanciaBUrl;

    // Puerto numerico de la Instancia B (por defecto 8082).
    @Value("${monitor.destinos.instancia-b.puerto:8082}")
    private int instanciaBPuerto;

    // Almacenamiento atomico en memoria del ultimo estado conocido para lecturas concurrentes sin bloqueos.
    private final AtomicReference<EstadoGeneralResponse> ultimoEstado = new AtomicReference<>();

    // Estados previos de cada componente para detectar unicamente transiciones reales (sin alertas duplicadas).
    private String previoNginx = null;
    private String previoInstanciaA = null;
    private String previoInstanciaB = null;
    private String previoEstadoGeneral = null;

    // Historial sincronizado y acotado a MAX_ALERTAS (20) alertas recientes en memoria.
    private final List<AlertaDto> historialAlertas = Collections.synchronizedList(new LinkedList<>());

    /**
     * Constructor con inyeccion del cliente HTTP de verificacion de salud.
     *
     * @param client componente HealthCheckClient con timeouts finitos y validacion de Actuator.
     */
    public MonitorService(HealthCheckClient client) {
        // Almacena el cliente HTTP inmutable.
        this.client = client;
    }

    /**
     * Tarea programada: se ejecuta periodicamente cada 5 segundos.
     * Mide salud, latencia, evalua reglas de estado agregado y registra transiciones.
     * [Actividad 3 y Actividad 11 - P23/P25/P26]: Ejecutada sobre el pool dedicado
     * monitor-scheduler-* definido en AsyncSchedulingConfig.
     */
    // Programa la ejecucion automatica de este metodo cada 5000 milisegundos (5 segundos).
    @Scheduled(fixedRate = 5000)
    public void ejecutarSondeoPeriodico() {
        // Captura la marca temporal actual en formato ISO-8601 con desplazamiento horario.
        String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 1. Comprobacion de NGINX (/nginx-ping en :8090) exigiendo HTTP 200 y {"status":"UP"}.
        HealthCheckClient.HealthResult resNginx = client.check(nginxUrl);
        // Extrae el estado evaluado de NGINX (UP o DOWN).
        String estadoNginx = resNginx.getEstado();

        // 2. Comprobacion de Instancia A (/actuator/health en 8080) exigiendo {"status":"UP"}.
        HealthCheckClient.HealthResult resA = client.check(instanciaAUrl);
        // Extrae el estado evaluado de la Instancia A.
        String estadoA = resA.getEstado();
        // Construye el DTO InstanciaInfo con puerto, URL, estado, latencia (o null si DOWN) y timestamp.
        InstanciaInfo infoA = new InstanciaInfo(instanciaAPuerto, instanciaAUrl, estadoA, resA.getLatenciaMs(), timestamp);

        // 3. Comprobacion de Instancia B (/actuator/health en 8082) exigiendo {"status":"UP"}.
        HealthCheckClient.HealthResult resB = client.check(instanciaBUrl);
        // Extrae el estado evaluado de la Instancia B.
        String estadoB = resB.getEstado();
        // Construye el DTO InstanciaInfo para la Instancia B.
        InstanciaInfo infoB = new InstanciaInfo(instanciaBPuerto, instanciaBUrl, estadoB, resB.getLatenciaMs(), timestamp);

        // Agrupa ambas instancias en una lista inmutable ordenada (A y B).
        List<InstanciaInfo> instancias = List.of(infoA, infoB);
        // Cuenta cuantas instancias de Vehiculos reportan estado UP (0, 1 o 2).
        long upCount = (estadoA.equalsIgnoreCase("UP") ? 1 : 0) + (estadoB.equalsIgnoreCase("UP") ? 1 : 0);

        // 4. [Actividad 3 y Actividad 11 - P23/P25/P26] Reglas de Estado General Agregado (Tabla 2 de la guia APE 04).
        String estadoGeneral;
        // Si NGINX no esta UP, la puerta de entrada fallo: estado critico NGINX_DOWN.
        if (!"UP".equalsIgnoreCase(estadoNginx)) {
            estadoGeneral = "NGINX_DOWN";
        // [P23 y P26] NGINX UP y las 2 instancias UP: operacion saludable en estado NORMAL.
        } else if (upCount == 2) {
            estadoGeneral = "NORMAL";
        // [P25] NGINX UP y solo 1 instancia UP: el servicio sigue operando en failover en estado DEGRADADO.
        } else if (upCount == 1) {
            estadoGeneral = "DEGRADADO";
        // NGINX UP pero 0 instancias UP: sin backends disponibles para atender Vehiculos.
        } else {
            estadoGeneral = "SIN_BACKENDS";
        }

        // 5. [Actividad 11 - P25/P26] Deteccion de transiciones reales y registro de alertas (sin duplicados consecutivos).
        detectarTransicion("NGINX", previoNginx, estadoNginx, timestamp);
        detectarTransicion("Vehiculos_8080", previoInstanciaA, estadoA, timestamp);
        detectarTransicion("Vehiculos_8082", previoInstanciaB, estadoB, timestamp);
        detectarTransicion("ESTADO_GENERAL", previoEstadoGeneral, estadoGeneral, timestamp);

        // Actualiza los estados previos para la comparacion del siguiente ciclo de 5 segundos.
        this.previoNginx = estadoNginx;
        this.previoInstanciaA = estadoA;
        this.previoInstanciaB = estadoB;
        this.previoEstadoGeneral = estadoGeneral;

        // Crea la nueva instantanea consolidada de estado general.
        EstadoGeneralResponse respuesta = new EstadoGeneralResponse(estadoNginx, estadoGeneral, instancias, timestamp);
        // Publica atomicamente la nueva instantanea para todos los hilos que consulten /api/monitor/estado o /instancias.
        this.ultimoEstado.set(respuesta);

        // Emite una linea resumida en bitacora para seguimiento operativo de cada ciclo de sondeo.
        log.info("[MONITOR] Sondeo cada 5s -> NGINX: {}, A(8080): {} ({}ms), B(8082): {} ({}ms) => ESTADO: {}",
                estadoNginx, estadoA, resA.getLatenciaMs(), estadoB, resB.getLatenciaMs(), estadoGeneral);
    }

    /**
     * Compara el estado anterior y el nuevo de un componente y genera una alerta
     * solo cuando existio un cambio efectivo (ignorando la primera lectura inicial).
     *
     * @param componente nombre del componente monitoreado.
     * @param previo     estado del ciclo previo (null en la primera ejecucion).
     * @param nuevo      estado detectado en el ciclo actual.
     * @param timestamp  marca temporal del sondeo actual.
     */
    private void detectarTransicion(String componente, String previo, String nuevo, String timestamp) {
        // Evita falsas alertas en el arranque (previo != null) y suprime alertas repetidas mientras el estado no cambie.
        if (previo != null && !previo.equalsIgnoreCase(nuevo)) {
            // Formatea el mensaje claro indicando componente, estado anterior y estado nuevo.
            String mensaje = String.format("Transicion detectada en %s: %s -> %s", componente, previo, nuevo);
            // Instancia el DTO de alerta auditable.
            AlertaDto alerta = new AlertaDto(timestamp, componente, previo, nuevo, mensaje);
            // Inserta la alerta al inicio del historial acotado en memoria.
            agregarAlerta(alerta);
            // Registra el incidente en nivel WARN para rapida identificacion en logs.
            log.warn("[ALERTA] {}", mensaje);
        }
    }

    /**
     * Inserta una nueva alerta al inicio de la lista (orden inverso) y recorta el
     * historial para no exceder MAX_ALERTAS (20 elementos).
     *
     * @param alerta evento de transicion a almacenar.
     */
    private synchronized void agregarAlerta(AlertaDto alerta) {
        // Inserta en indice 0 para que GET /api/monitor/alertas muestre primero los incidentes mas recientes.
        historialAlertas.add(0, alerta); // insertar al inicio para ver las mas recientes primero
        // Elimina las alertas mas antiguas si la lista supera el tope configurado de 20 registros.
        while (historialAlertas.size() > MAX_ALERTAS) {
            historialAlertas.remove(historialAlertas.size() - 1);
        }
    }

    /**
     * Devuelve el ultimo estado general calculado por el sondeo periodico.
     * Si aun no ha corrido el primer ciclo, dispara un sondeo inmediato bajo demanda.
     *
     * @return EstadoGeneralResponse actualizado.
     */
    public EstadoGeneralResponse consultarEstadoGeneral() {
        // Lee de forma atomica la ultima instantanea almacenada.
        EstadoGeneralResponse actual = ultimoEstado.get();
        // Si el endpoint se consulta antes del primer ciclo programado, fuerza un sondeo inicial sincrono.
        if (actual == null) {
            ejecutarSondeoPeriodico();
            return ultimoEstado.get();
        }
        // Retorna la instantanea mas reciente sin generar llamadas extra a los backends.
        return actual;
    }

    /**
     * Devuelve el listado de salud y latencia de las instancias individuales (8080 y 8082).
     *
     * @return lista de InstanciaInfo o lista vacia si no existiera estado.
     */
    public List<InstanciaInfo> consultarInstancias() {
        // Reutiliza la instantanea consolidada de estado general.
        EstadoGeneralResponse actual = consultarEstadoGeneral();
        // Retorna la lista de instancias o una coleccion vacia segura en caso de ausencia.
        return actual != null ? actual.getInstancias() : Collections.emptyList();
    }

    /**
     * Devuelve una copia defensiva del historial de alertas de transicion en memoria.
     *
     * @return nueva lista con las alertas ordenadas de la mas reciente a la mas antigua.
     */
    public List<AlertaDto> consultarAlertas() {
        // Sincroniza sobre la lista compartida durante la copia para garantizar consistencia ante escrituras concurrentes.
        synchronized (historialAlertas) {
            return new ArrayList<>(historialAlertas);
        }
    }
}
