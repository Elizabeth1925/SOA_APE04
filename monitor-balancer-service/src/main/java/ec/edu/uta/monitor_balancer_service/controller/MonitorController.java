/**
 * ==============================================================================
 * ARCHIVO: MonitorController.java
 * COMPONENTE: monitor-balancer-service (Controlador REST - Puerto 8083)
 * ROL: Expone los 5 endpoints de solo lectura bajo /api/monitor/* para consultar
 *      el estado agregado del sistema, la salud y latencia de cada instancia,
 *      la distribucion real del balanceo 3:1 en access.log, las estadisticas de
 *      codigos HTTP/latencia y el historial de alertas de transiciones UP/DOWN.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3]: Exposicion completa de la API independiente de monitoreo
 *     (/estado, /instancias, /balanceo, /estadisticas, /alertas) en puerto 8083.
 *   - [Actividad 11 - P23]: /estado y /instancias evidencian NGINX UP y ambas
 *     instancias UP con estadoGeneral=NORMAL.
 *   - [Actividad 11 - P24]: /balanceo y /estadisticas evidencian la proporcion
 *     ponderada 3:1 sobre la ventana temporal activa.
 *   - [Actividad 11 - P25]: /estado, /instancias y /alertas evidencian estado
 *     DEGRADADO, instancia caida DOWN y alerta de transicion tras apagar un nodo.
 *   - [Actividad 11 - P26]: /estado, /instancias y /alertas evidencian retorno
 *     automatico a NORMAL y alerta de recuperacion DOWN -> UP.
 * ==============================================================================
 */
// Declara el paquete de controladores REST del servicio de monitoreo.
package ec.edu.uta.monitor_balancer_service.controller;

// Importa List para tipar las respuestas de colecciones de instancias y alertas.
import java.util.List;

// Importa ResponseEntity para construir respuestas HTTP 200 OK con cuerpos tipados en JSON.
import org.springframework.http.ResponseEntity;
// Importa @GetMapping para asociar cada metodo a su ruta HTTP GET especifica.
import org.springframework.web.bind.annotation.GetMapping;
// Importa @RequestMapping para definir el prefijo comun /api/monitor.
import org.springframework.web.bind.annotation.RequestMapping;
// Importa @RequestParam para leer parametros opcionales de ventana temporal (ventanaMinutos y desde).
import org.springframework.web.bind.annotation.RequestParam;
// Importa @RestController para exponer el controlador con serializacion JSON automatica.
import org.springframework.web.bind.annotation.RestController;

// Importa el DTO de alerta de transicion de estado.
import ec.edu.uta.monitor_balancer_service.dto.AlertaDto;
// Importa el DTO con el desglose de solicitudes y porcentajes de balanceo 3:1.
import ec.edu.uta.monitor_balancer_service.dto.BalanceoResponse;
// Importa el DTO con conteos HTTP 2xx/4xx/5xx y promedio de respuesta en ms.
import ec.edu.uta.monitor_balancer_service.dto.EstadisticasResponse;
// Importa el DTO del estado agregado general (NORMAL, DEGRADADO, SIN_BACKENDS, NGINX_DOWN).
import ec.edu.uta.monitor_balancer_service.dto.EstadoGeneralResponse;
// Importa el DTO con la informacion individual de salud y latencia por instancia.
import ec.edu.uta.monitor_balancer_service.dto.InstanciaInfo;
// Importa el servicio de sondeo periodico y gestion de alertas en memoria.
import ec.edu.uta.monitor_balancer_service.service.MonitorService;
// Importa el servicio de parseo de access.log con filtrado por ventana temporal.
import ec.edu.uta.monitor_balancer_service.service.NginxLogParserService;

/**
 * Controlador REST que centraliza los 5 endpoints de observabilidad en /api/monitor.
 */
// Marca esta clase como controlador REST de Spring MVC.
@RestController
// [Actividad 3] Define la ruta base /api/monitor atendida en el puerto 8083.
@RequestMapping("/api/monitor")
public class MonitorController {

    // Servicio encargado del sondeo cada 5 segundos, estado agregado y registro de alertas.
    private final MonitorService monitorService;
    // Servicio encargado de leer y agregar metricas desde el archivo access.log de NGINX.
    private final NginxLogParserService logParserService;

    /**
     * Constructor con inyeccion de dependencias administrado por Spring.
     *
     * @param monitorService   servicio de salud y alertas en tiempo real.
     * @param logParserService servicio analizador del log de acceso de NGINX.
     */
    public MonitorController(MonitorService monitorService, NginxLogParserService logParserService) {
        // Almacena la referencia inmutable a MonitorService.
        this.monitorService = monitorService;
        // Almacena la referencia inmutable a NginxLogParserService.
        this.logParserService = logParserService;
    }

    /**
     * Endpoint 1: Estado general del sistema.
     * [Actividad 3 y Actividad 11 - P23/P25/P26]: Expone el estado de NGINX,
     * estadoGeneral (NORMAL, DEGRADADO, SIN_BACKENDS o NGINX_DOWN) y lista de instancias.
     *
     * @return HTTP 200 OK con el ultimo EstadoGeneralResponse calculado.
     */
    // Mapea GET /api/monitor/estado.
    @GetMapping("/estado")
    public ResponseEntity<EstadoGeneralResponse> obtenerEstado() {
        // Consulta la instantanea mas reciente del estado general y la devuelve con HTTP 200.
        return ResponseEntity.ok(monitorService.consultarEstadoGeneral());
    }

    /**
     * Endpoint 2: Salud y latencias de instancias individuales.
     * [Actividad 3 y Actividad 11 - P23/P25/P26]: Reporta puerto, URL, estado y
     * latenciaMs (o null si esta DOWN) para las instancias 8080 y 8082.
     *
     * @return HTTP 200 OK con la lista de InstanciaInfo.
     */
    // Mapea GET /api/monitor/instancias.
    @GetMapping("/instancias")
    public ResponseEntity<List<InstanciaInfo>> obtenerInstancias() {
        // Obtiene el detalle de salud de cada instancia monitoreada y responde con HTTP 200.
        return ResponseEntity.ok(monitorService.consultarInstancias());
    }

    /**
     * Endpoint 3: Metricas de balanceo de carga real calculadas desde access.log con ventana temporal.
     * [Actividad 3 y Actividad 11 - P24/P25]: Permite auditar la distribucion real entre
     * puerto 8080 (peso 3) y puerto 8082 (peso 1) dentro de la corrida de prueba actual.
     *
     * @param ventanaMinutos cantidad opcional de minutos hacia atras a analizar.
     * @param desde          timestamp ISO-8601 opcional de inicio exacto de la corrida.
     * @return HTTP 200 OK con BalanceoResponse.
     */
    // Mapea GET /api/monitor/balanceo aceptando filtros opcionales de ventana temporal.
    @GetMapping("/balanceo")
    public ResponseEntity<BalanceoResponse> obtenerBalanceo(
            // Parametro query opcional ?ventanaMinutos=N para acotar los ultimos N minutos.
            @RequestParam(required = false) Integer ventanaMinutos,
            // Parametro query opcional ?desde=2026-... en formato ISO-8601 con offset.
            @RequestParam(required = false) String desde) {
        // Delega el calculo de reparto por upstream al parser de access.log y responde HTTP 200.
        return ResponseEntity.ok(logParserService.calcularBalanceo(ventanaMinutos, desde));
    }

    /**
     * Endpoint 4: Estadisticas cuantitativas de codigos HTTP y tiempos de respuesta con ventana temporal.
     * [Actividad 3 y Actividad 11 - P24/P25/P26]: Reporta total de peticiones, conteos 2xx/4xx/5xx,
     * tiempo promedio en milisegundos y criterio de ventana aplicado.
     *
     * @param ventanaMinutos cantidad opcional de minutos hacia atras a analizar.
     * @param desde          timestamp ISO-8601 opcional de inicio exacto de la corrida.
     * @return HTTP 200 OK con EstadisticasResponse.
     */
    // Mapea GET /api/monitor/estadisticas aceptando filtros opcionales de ventana temporal.
    @GetMapping("/estadisticas")
    public ResponseEntity<EstadisticasResponse> obtenerEstadisticas(
            // Parametro query opcional ?ventanaMinutos=N.
            @RequestParam(required = false) Integer ventanaMinutos,
            // Parametro query opcional ?desde=ISO-8601.
            @RequestParam(required = false) String desde) {
        // Delega el calculo de estadisticas HTTP y latencia media a NginxLogParserService y retorna HTTP 200.
        return ResponseEntity.ok(logParserService.calcularEstadisticas(ventanaMinutos, desde));
    }

    /**
     * Endpoint 5: Consulta de incidentes y alertas activas.
     * [Actividad 3 y Actividad 11 - P25/P26]: Devuelve el historial en memoria
     * (acotado a 20 eventos, mas recientes primero) de transiciones UP/DOWN y
     * cambios en el estado general del sistema.
     *
     * @return HTTP 200 OK con la lista de AlertaDto registradas.
     */
    // Mapea GET /api/monitor/alertas.
    @GetMapping("/alertas")
    public ResponseEntity<List<AlertaDto>> obtenerAlertas() {
        // Recupera una copia segura del historial de transiciones y la devuelve con HTTP 200.
        return ResponseEntity.ok(monitorService.consultarAlertas());
    }
}
