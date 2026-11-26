/**
 * ==============================================================================
 * ARCHIVO: NginxLogParserService.java
 * COMPONENTE: monitor-balancer-service (Analizador de Logs de NGINX)
 * ROL: Lee y parsea el archivo access.log de NGINX (formato monitor_format)
 *      aplicando filtros estrictos de ventana temporal (por minutos o desde un
 *      timestamp ISO-8601), excluye /nginx-ping, resuelve el upstream final en
 *      escenarios de reintento pasivo y calcula las metricas de balanceo 3:1 y
 *      estadisticas cuantitativas HTTP.
 * ACTIVIDADES APE 04 CUBIERTAS:
 *   - [Actividad 3]: Lectura desacoplada de access.log sin interferir en el
 *     trafico en vivo de NGINX ni de los microservicios transaccionales.
 *   - [Actividad 11 - P24]: Calcula la proporcion real entre puerto 8080
 *     (weight=3) y puerto 8082 (weight=1) unicamente dentro de la ventana de la
 *     corrida de prueba actual.
 *   - [Actividad 11 - P25/P26]: Reconoce reintentos pasivos de NGINX
 *     ("127.0.0.1:8082, 127.0.0.1:8080") tomando el ultimo upstream efectivo y
 *     reporta conteos 2xx/4xx/5xx y latencia promedio en ms.
 * ==============================================================================
 */
// Declara el paquete de servicios de monitor-balancer-service.
package ec.edu.uta.monitor_balancer_service.service;

// Importa File para verificar existencia y permisos de lectura del archivo access.log.
import java.io.File;
// Importa StandardCharsets para leer el log con codificacion UTF-8 consistente.
import java.nio.charset.StandardCharsets;
// Importa Files para leer todas las lineas del archivo access.log de NGINX.
import java.nio.file.Files;
// Importa OffsetDateTime para parsear y comparar fechas ISO-8601 con zona horaria.
import java.time.OffsetDateTime;
// Importa ArrayList para acumular las entradas filtradas dentro de la ventana temporal.
import java.util.ArrayList;
// Importa List para manejar colecciones de lineas y entradas parseadas.
import java.util.List;

// Importa la interfaz Logger de SLF4J para advertencias de formato y errores de lectura.
import org.slf4j.Logger;
// Importa la fabrica de loggers SLF4J.
import org.slf4j.LoggerFactory;
// Importa @Value para inyectar ruta del log, puertos A/B y ventana temporal por defecto.
import org.springframework.beans.factory.annotation.Value;
// Importa @Service para registrar el analizador como bean singleton de Spring.
import org.springframework.stereotype.Service;

// Importa el DTO de salida con el reparto de carga por instancia.
import ec.edu.uta.monitor_balancer_service.dto.BalanceoResponse;
// Importa el DTO de salida con estadisticas de codigos HTTP y tiempo promedio de respuesta.
import ec.edu.uta.monitor_balancer_service.dto.EstadisticasResponse;

/**
 * Servicio encargado de parsear el archivo access.log de NGINX.
 * CORREGIDO (APE 04): Aplica ventana temporal real (por minutos o timestamp ISO-8601 de inicio),
 * garantizando que los calculos de balanceo y estadisticas cuantifiquen UNICAMENTE la corrida de prueba actual
 * y no incluyan peticiones residuales de ejecuciones anteriores.
 */
// Registra el servicio analizador de logs en el contenedor Spring.
@Service
public class NginxLogParserService {

    // Logger estatico para registrar advertencias de archivo ausente, formatos invalidos o errores de E/S.
    private static final Logger log = LoggerFactory.getLogger(NginxLogParserService.class);

    // Ruta del archivo access.log generada por NGINX (parametrizable desde application.properties).
    @Value("${monitor.nginx.log-path:C:/nginx/logs/access.log}")
    private String logPath;

    // Puerto de la Instancia A de Vehiculos (peso 3 en NGINX, por defecto 8080).
    @Value("${monitor.destinos.instancia-a.puerto:8080}")
    private int puertoA;

    // Puerto de la Instancia B de Vehiculos (peso 1 en NGINX, por defecto 8082).
    @Value("${monitor.destinos.instancia-b.puerto:8082}")
    private int puertoB;

    /**
     * Ventana temporal por defecto en minutos (30 minutos para sesion de prueba activa).
     */
    // Minutos hacia atras considerados cuando el cliente no especifica ?ventanaMinutos ni ?desde.
    @Value("${monitor.ventana.minutos-defecto:30}")
    private int ventanaMinutosDefecto;

    /**
     * Modelo interno que representa una linea parseada del access.log.
     */
    public static class LogEntry {
        // Marca temporal textual original en formato ISO-8601 escrita por NGINX ($time_iso8601).
        public String timestamp;
        // Instancia OffsetDateTime parseada para comparacion cronologica contra la ventana temporal.
        public OffsetDateTime parsedTime;
        // Metodo HTTP de la solicitud ($request_method: GET, POST, PUT, DELETE, etc.).
        public String method;
        // Ruta limpia sin parametros de consulta ($uri) registrada por el log seguro monitor_format.
        public String uri;
        // Codigo de estado HTTP devuelto al cliente ($status).
        public int status;
        // Ultimo upstream que efectivamente atendio la solicitud tras eventuales reintentos pasivos.
        public String finalUpstream;
        // Tiempo total de procesamiento de la solicitud convertido de segundos a milisegundos.
        public double requestTimeMs;

        /**
         * Constructor completo de una entrada valida del log de acceso.
         *
         * @param timestamp     cadena temporal ISO-8601 original.
         * @param parsedTime    fecha/hora parseada para comparacion temporal.
         * @param method        verbo HTTP.
         * @param uri           ruta sin query strings.
         * @param status        codigo HTTP de respuesta.
         * @param finalUpstream direccion IP:puerto del upstream efectivo (o "-").
         * @param requestTimeMs tiempo total de solicitud en milisegundos.
         */
        public LogEntry(String timestamp, OffsetDateTime parsedTime, String method, String uri, int status, String finalUpstream, double requestTimeMs) {
            // Asigna la marca temporal original.
            this.timestamp = timestamp;
            // Asigna la fecha/hora parseada.
            this.parsedTime = parsedTime;
            // Asigna el metodo HTTP.
            this.method = method;
            // Asigna la URI limpia.
            this.uri = uri;
            // Asigna el codigo HTTP.
            this.status = status;
            // Asigna el upstream efectivo final.
            this.finalUpstream = finalUpstream;
            // Asigna la duracion en milisegundos.
            this.requestTimeMs = requestTimeMs;
        }
    }

    /**
     * Lee y parsea las lineas del access.log de NGINX aplicando una ventana temporal estricta.
     * [Actividad 3 y Actividad 11 - P24/P25]: Garantiza que las metricas reflejen unicamente
     * el intervalo de prueba solicitado y descarta sondas /nginx-ping.
     *
     * @param ventanaMinutos minutos hacia atras a considerar (si es null, usa ventanaMinutosDefecto).
     * @param desdeIso       timestamp ISO-8601 de inicio explicito (opcional; tiene prioridad si es valido).
     * @return lista ordenada de LogEntry que cumplen con el formato y la ventana temporal.
     */
    public List<LogEntry> leerRegistros(Integer ventanaMinutos, String desdeIso) {
        // Inicializa la lista de entradas validas recolectadas.
        List<LogEntry> entries = new ArrayList<>();
        // Construye el descriptor del archivo access.log configurado.
        File file = new File(logPath);
        // Si el archivo no existe o no tiene permiso de lectura, advierte y devuelve lista vacia sin fallar.
        if (!file.exists() || !file.canRead()) {
            log.warn("El archivo de logs de NGINX no existe o no se puede leer: {}", logPath);
            return entries;
        }

        // Determina el limite temporal inferior a partir del cual se incluiran las lineas del log.
        OffsetDateTime limiteInferior = null;
        // Prioridad 1: si el usuario envio el parametro ?desde en formato ISO-8601, intenta parsearlo.
        if (desdeIso != null && !desdeIso.trim().isEmpty()) {
            try {
                limiteInferior = OffsetDateTime.parse(desdeIso.trim());
            } catch (Exception ex) {
                // Si el formato de 'desde' es invalido, registra advertencia y cae al respaldo por minutos.
                log.warn("Formato invalido para parametro 'desde': {}. Ignorando.", desdeIso);
            }
        }
        // Prioridad 2: si no hubo 'desde' valido, calcula el limite restando ventanaMinutos (o 30 min por defecto).
        if (limiteInferior == null) {
            int minutos = (ventanaMinutos != null && ventanaMinutos > 0) ? ventanaMinutos : ventanaMinutosDefecto;
            limiteInferior = OffsetDateTime.now().minusMinutes(minutos);
        }

        // Lee y procesa linea por linea el archivo access.log en codificacion UTF-8.
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (String line : lines) {
                // Elimina espacios y saltos residuales al inicio y final de la linea.
                line = line.trim();
                // Omite lineas en blanco.
                if (line.isEmpty()) continue;

                // Formato monitor_format de nginx.conf: time_iso8601|method|uri|status|upstream_addr|upstream_status|request_time|upstream_response_time
                String[] parts = line.split("\\|");
                // Descarta lineas incompletas o de formatos antiguos que no tengan los 8 campos separados por "|".
                if (parts.length < 8) {
                    continue;
                }

                // Campo 0: marca temporal ISO-8601 emitida por NGINX.
                String timestamp = parts[0];
                OffsetDateTime entryTime;
                // Parsea el timestamp; si la linea esta corrupta la ignora sin interrumpir el resto del archivo.
                try {
                    entryTime = OffsetDateTime.parse(timestamp);
                } catch (Exception ex) {
                    continue;
                }

                // [Actividad 11 - P24] FILTRO DE VENTANA TEMPORAL: descarta lineas anteriores al inicio de la prueba activa.
                if (entryTime.isBefore(limiteInferior)) {
                    continue;
                }

                // Campo 1: metodo HTTP (GET, POST, etc.).
                String method = parts[1];
                // Campo 2: URI limpia ($uri sin query strings sensibles).
                String uri = parts[2];
                // Campo 3: codigo de estado HTTP final devuelto al cliente.
                int status;
                try {
                    status = Integer.parseInt(parts[3]);
                } catch (NumberFormatException e) {
                    continue;
                }

                // Filtra /nginx-ping para que las sondas de salud nunca alteren metricas de trafico de negocio.
                if (uri.startsWith("/nginx-ping")) {
                    continue;
                }

                // [Actividad 3 y Actividad 11 - P25] Campo 4: en reintentos pasivos, NGINX concatena upstreams separados por coma: "127.0.0.1:8082, 127.0.0.1:8080".
                String upstreamAddrRaw = parts[4].trim();
                String finalUpstream = "-";
                // Si NGINX registro uno o mas upstreams, selecciona el ultimo elemento (el nodo que finalmente atendio).
                if (!upstreamAddrRaw.isEmpty() && !upstreamAddrRaw.equals("-")) {
                    String[] upstreams = upstreamAddrRaw.split(",\\s*");
                    finalUpstream = upstreams[upstreams.length - 1]; // ultimo upstream que efectivamente atendio
                }

                // Campo 6: request_time viene en segundos (ej. "0.015"); se convierte a milisegundos multiplicando por 1000.
                double requestTimeMs = 0.0;
                try {
                    requestTimeMs = Double.parseDouble(parts[6]) * 1000.0;
                } catch (NumberFormatException ignored) {}

                // Agrega la entrada valida y filtrada a la lista de resultados.
                entries.add(new LogEntry(timestamp, entryTime, method, uri, status, finalUpstream, requestTimeMs));
            }
        } catch (Exception e) {
            // Registra cualquier error de lectura de archivo sin tumbar el endpoint de monitoreo.
            log.error("Error al leer el archivo de logs de NGINX: {}", e.getMessage(), e);
        }

        // Retorna todas las entradas comprendidas en la ventana temporal solicitada.
        return entries;
    }

    /**
     * Sobrecarga de conveniencia que lee registros usando la ventana por defecto (30 minutos).
     *
     * @return lista de entradas dentro de la ventana por defecto.
     */
    public List<LogEntry> leerRegistros() {
        // Delega pasando parametros nulos para aplicar ventanaMinutosDefecto.
        return leerRegistros(null, null);
    }

    /**
     * Calcula metricas de balanceo aplicando ventana temporal real.
     * [Actividad 3 y Actividad 11 - P24/P25]: Clasifica cada solicitud segun su
     * upstream efectivo final (:8080, :8082 u otros) y calcula porcentajes redondeados.
     *
     * @param ventanaMinutos minutos hacia atras a analizar (opcional).
     * @param desdeIso       timestamp ISO-8601 de inicio (opcional).
     * @return BalanceoResponse con conteos y porcentajes reales de distribucion.
     */
    public BalanceoResponse calcularBalanceo(Integer ventanaMinutos, String desdeIso) {
        // Obtiene los registros filtrados del access.log dentro de la ventana solicitada.
        List<LogEntry> entries = leerRegistros(ventanaMinutos, desdeIso);
        // Si no existen solicitudes en la ventana, devuelve una respuesta inicializada en ceros.
        if (entries.isEmpty()) {
            return new BalanceoResponse(null, null, 0, 0, 0, 0, 0.0, 0.0);
        }

        // Toma las marcas temporales del primer y ultimo registro efectivamente encontrados en la muestra.
        String inicio = entries.get(0).timestamp;
        String fin = entries.get(entries.size() - 1).timestamp;

        // Contadores de solicitudes por instancia A (:8080), instancia B (:8082) y otras rutas sin upstream de vehiculos.
        long solicitudesA = 0;
        long solicitudesB = 0;
        long sinUpstream = 0;

        // Patrones de sufijo de puerto para identificar cada instancia en la direccion IP:puerto.
        String patternA = ":" + puertoA;
        String patternB = ":" + puertoB;

        // Recorre todas las entradas de la ventana y acumula cada contador.
        for (LogEntry e : entries) {
            if (e.finalUpstream.contains(patternA)) {
                solicitudesA++;
            } else if (e.finalUpstream.contains(patternB)) {
                solicitudesB++;
            } else {
                sinUpstream++;
            }
        }

        // Calcula el total de registros y los porcentajes de A y B redondeados a 2 decimales.
        long total = entries.size();
        double porcentajeA = total > 0 ? Math.round((solicitudesA * 100.0 / total) * 100.0) / 100.0 : 0.0;
        double porcentajeB = total > 0 ? Math.round((solicitudesB * 100.0 / total) * 100.0) / 100.0 : 0.0;

        // Construye y devuelve el DTO completo con el reparto de carga observado.
        return new BalanceoResponse(inicio, fin, total, solicitudesA, solicitudesB, sinUpstream, porcentajeA, porcentajeB);
    }

    /**
     * Sobrecarga de conveniencia que calcula el balanceo con la ventana temporal por defecto.
     *
     * @return BalanceoResponse de los ultimos 30 minutos.
     */
    public BalanceoResponse calcularBalanceo() {
        // Delega en calcularBalanceo con parametros por defecto.
        return calcularBalanceo(null, null);
    }

    /**
     * Calcula estadisticas cuantitativas aplicando ventana temporal real.
     * [Actividad 3 y Actividad 11 - P24/P25/P26]: Contabiliza respuestas 2xx, 4xx y 5xx
     * y determina el tiempo promedio de respuesta en milisegundos.
     *
     * @param ventanaMinutos minutos hacia atras a analizar (opcional).
     * @param desdeIso       timestamp ISO-8601 de inicio (opcional).
     * @return EstadisticasResponse con conteos por clase de codigo HTTP y latencia media.
     */
    public EstadisticasResponse calcularEstadisticas(Integer ventanaMinutos, String desdeIso) {
        // Construye una descripcion explicita del criterio de ventana aplicado para documentar la respuesta JSON.
        String criterio = (desdeIso != null && !desdeIso.trim().isEmpty())
                ? "Ventana explicita desde " + desdeIso.trim()
                : "Ventana de ultimos " + ((ventanaMinutos != null && ventanaMinutos > 0) ? ventanaMinutos : ventanaMinutosDefecto) + " minutos";

        // Recupera las entradas filtradas del access.log para la ventana indicada.
        List<LogEntry> entries = leerRegistros(ventanaMinutos, desdeIso);
        // Si no hay datos en la ventana, retorna contadores en cero preservando el criterio aplicado.
        if (entries.isEmpty()) {
            return new EstadisticasResponse(null, null, 0, 0, 0, 0, 0.0, criterio);
        }

        // Extrae el rango temporal real cubierto por las entradas encontradas.
        String inicio = entries.get(0).timestamp;
        String fin = entries.get(entries.size() - 1).timestamp;

        // Inicializa los acumuladores de codigos de estado HTTP y la suma total de latencias.
        long count2xx = 0;
        long count4xx = 0;
        long count5xx = 0;
        double sumaTiempo = 0.0;

        // Clasifica cada entrada por familia de codigo HTTP y acumula su tiempo de respuesta en ms.
        for (LogEntry e : entries) {
            int code = e.status;
            if (code >= 200 && code < 300) count2xx++;
            else if (code >= 400 && code < 500) count4xx++;
            else if (code >= 500 && code < 600) count5xx++;

            sumaTiempo += e.requestTimeMs;
        }

        // Calcula el total de peticiones y la latencia promedio en ms redondeada a 2 decimales.
        long total = entries.size();
        double avgTiempo = total > 0 ? Math.round((sumaTiempo / total) * 100.0) / 100.0 : 0.0;

        // Devuelve el resumen cuantitativo completo para GET /api/monitor/estadisticas.
        return new EstadisticasResponse(inicio, fin, total, count2xx, count4xx, count5xx, avgTiempo, criterio);
    }

    /**
     * Sobrecarga de conveniencia que calcula estadisticas sobre la ventana temporal por defecto.
     *
     * @return EstadisticasResponse de los ultimos 30 minutos.
     */
    public EstadisticasResponse calcularEstadisticas() {
        // Delega en calcularEstadisticas usando la ventana por defecto.
        return calcularEstadisticas(null, null);
    }
}
