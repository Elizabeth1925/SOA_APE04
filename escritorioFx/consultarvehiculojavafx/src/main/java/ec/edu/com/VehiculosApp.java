/*
 * ============================================================================
 * Archivo: VehiculosApp.java
 * Proyecto: Cliente de Escritorio JavaFX (consultarvehiculojavafx) - SOA APE 04
 * Función:
 *   Interfaz gráfica principal en JavaFX con dos pestañas operativas:
 *   1) Catálogo de Vehículos: consulta protegida con JWT contra GET /api/vehiculos
 *      enrutado a través del Gateway NGINX (:8090).
 *   2) Notificaciones en Vivo (RabbitMQ): monitoreo automático mediante un
 *      temporizador Timeline cada 3 segundos hacia GET /api/notificaciones vía
 *      NGINX (:8090), sin conexión directa del escritorio a RabbitMQ ni a MySQL.
 * Actividades APE 04 cubiertas:
 *   - [Actividad 9]: Cliente JavaFX conectado vía NGINX (:8090), pestaña Catálogo
 *     con JWT, pestaña Notificaciones en Vivo con Timeline cada 3s, diferenciación
 *     de 3 estados visuales (verde conectado con historial, azul pantalla vacía
 *     saludable HTTP 200 [], rojo fallo de servicio HTTP 502/504 o error de red)
 *     y actualización segura del grafo de escena con Platform.runLater.
 *   - [Actividad 10 - P27]: Visualización automática de ventaId, vehiculoId,
 *     compradorId y operacionId generados desde la PWA, además de reflejar el
 *     estado actualizado del vehículo en el catálogo.
 *   - [Actividad 11]: Manejo explícito de HTTP 401/403 en vehículos y distinción
 *     clara de fallos HTTP 502/504 o caída de NGINX al detener notificaciones-service.
 * ============================================================================
 */
// Declara el paquete principal de la aplicación JavaFX.
package ec.edu.com;

// Importaciones de Java estándar para funciones lambdas y manejo de datos.
// Permite capturar la hora local exacta de cada sincronización exitosa o fallida.
import java.time.LocalTime;
// Formatea la hora de sincronización al patrón legible HH:mm:ss.
import java.time.format.DateTimeFormatter;
// Interfaz funcional empleada para extraer dinámicamente cada columna de los records DTO.
import java.util.function.Function;

// Importaciones de JavaFX para propiedades observables en tablas.
// Envuelve valores inmutables de records para vincularlos a celdas de TableView sin getters JavaBean.
import javafx.beans.property.ReadOnlyObjectWrapper;

// Importaciones base del ciclo de vida y despacho de hilos en JavaFX [Actividad 9].
// Define el intervalo y acción periódica ejecutada dentro del Timeline de sondeo.
import javafx.animation.KeyFrame;
// Temporizador de JavaFX usado para sondear GET /api/notificaciones cada 3 segundos [Actividad 9].
import javafx.animation.Timeline;
// Clase base obligatoria del ciclo de vida de toda aplicación JavaFX.
import javafx.application.Application;
// Utilidad para encolar actualizaciones de controles en el JavaFX Application Thread [Actividad 9].
import javafx.application.Platform;
// Unidad de duración de JavaFX para configurar el intervalo de 3 segundos del sondeo.
import javafx.util.Duration;

// Importaciones de diseño (layout), alineación y márgenes.
// Define márgenes internos (padding) alrededor de contenedores.
import javafx.geometry.Insets;
// Define constantes de alineación de controles dentro de contenedores HBox/VBox.
import javafx.geometry.Pos;
// Representa el contenido visual raíz de la ventana JavaFX.
import javafx.scene.Scene;
// Contenedor horizontal para agrupar botones, casillas y etiquetas de estado.
import javafx.scene.layout.HBox;
// Controla la prioridad de crecimiento vertical de tablas y pestañas al redimensionar.
import javafx.scene.layout.Priority;
// Contenedor vertical principal para apilar encabezados, controles y tablas.
import javafx.scene.layout.VBox;

// Importaciones de controles visuales de JavaFX.
// Botón de acción para disparar consultas manuales.
import javafx.scene.control.Button;
// Casilla de verificación para activar o pausar el sondeo automático cada 3s.
import javafx.scene.control.CheckBox;
// Etiqueta de texto para títulos, indicadores de estado y contadores.
import javafx.scene.control.Label;
// Campo enmascarado para ingresar el token JWT sin exponerlo visualmente en pantalla [Actividad 9].
import javafx.scene.control.PasswordField;
// Representa cada pestaña individual dentro del TabPane.
import javafx.scene.control.Tab;
// Contenedor de pestañas que separa el Catálogo de Vehículos y las Notificaciones en Vivo [Actividad 9].
import javafx.scene.control.TabPane;
// Define cada columna tipada dentro de las tablas JavaFX.
import javafx.scene.control.TableColumn;
// Tabla gráfica para desplegar listas de Vehiculo y NotificacionVentaDto.
import javafx.scene.control.TableView;

// Importación de colores oficial de JavaFX para los 3 estados visuales (verde, azul, rojo) [Actividad 9].
import javafx.scene.paint.Color;

// Importación de la ventana principal del sistema operativo en JavaFX.
import javafx.stage.Stage;

/**
 * Ventana principal de la aplicación JavaFX (SOA APE 04).
 * Permite la consulta de vehículos balanceados vía NGINX (3:1) y la supervisión
 * en tiempo real de notificaciones de ventas asíncronas emitidas por RabbitMQ.
 */
// Clase principal de la interfaz de escritorio que extiende javafx.application.Application [Actividad 9].
public final class VehiculosApp extends Application {

    // Instancia única del cliente HTTP encargado de invocar al Gateway NGINX (:8090) [Actividad 9].
    private final VehiculosApiClient api = new VehiculosApiClient();

    // Controles pestaña de Vehículos [Actividad 9].
    // Campo de texto enmascarado donde el operador pega el token JWT sin prefijo Bearer.
    private final PasswordField txtToken = new PasswordField();
    // Botón que dispara la petición autenticada GET /api/vehiculos.
    private final Button btnConsultar = new Button("Consultar Vehículos");
    // Etiqueta de retroalimentación sobre el estado HTTP o errores 401/403/red [Actividad 11].
    private final Label lblEstadoVehiculos = new Label("Listo para consultar vía NGINX");
    // Contador visible de registros cargados en la tabla de vehículos.
    private final Label lblTotalVehiculos = new Label("Total de Vehículos: 0");
    // Tabla que muestra el catálogo de objetos Vehiculo recibidos del backend.
    private final TableView<Vehiculo> tablaVehiculos = new TableView<>();

    // Controles pestaña de Notificaciones RabbitMQ [Actividad 9 / Actividad 10 - P27].
    // Tabla que presenta el historial en vivo de registros NotificacionVentaDto.
    private final TableView<NotificacionVentaDto> tablaNotificaciones = new TableView<>();
    // Botón para forzar una consulta inmediata de GET /api/notificaciones fuera del ciclo de 3s.
    private final Button btnActualizarNotificaciones = new Button("Actualizar Notificaciones");
    // Casilla que permite encender o pausar el sondeo automático cada 3 segundos [Actividad 9].
    private final CheckBox chkAutoRefresh = new CheckBox("Sondeo automático (cada 3s)");
    // Banner de estado visual con codificación verde (conectado), azul (vacío 200) y rojo (fallo 502/504) [Actividad 9 / Actividad 11].
    private final Label lblEstadoConexionNotif = new Label("Estado de conexión: Iniciando enlace con NGINX (:8090)...");
    // Contador del total de notificaciones recuperadas desde notificaciones-service.
    private final Label lblTotalNotificaciones = new Label("Notificaciones recibidas: 0");
    // Indicador de la hora de la última sincronización exitosa o intento fallido.
    private final Label lblUltimaSync = new Label("Última sincronización: —");

    // Referencia al temporizador Timeline que realiza el sondeo automático cada 3 segundos [Actividad 9].
    private Timeline temporizadorSync;

    // Método del ciclo de vida de JavaFX que arma y despliega la ventana principal [Actividad 9].
    @Override
    public void start(Stage ventana) {
        // Crea el panel de pestañas principal de la aplicación.
        TabPane panelPestanas = new TabPane();
        // Impide que el usuario cierre accidentalmente cualquiera de las dos pestañas operativas.
        panelPestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // PESTAÑA 1: CATÁLOGO DE VEHÍCULOS (GET /api/vehiculos con JWT vía NGINX :8090) [Actividad 9].
        Tab pestanaVehiculos = new Tab("🚗 Catálogo de Vehículos", construirVistaVehiculos());

        // PESTAÑA 2: NOTIFICACIONES DE VENTAS RABBITMQ (GET /api/notificaciones vía NGINX :8090) [Actividad 9 / Actividad 10 - P27].
        Tab pestanaNotificaciones = new Tab("🔔 Notificaciones en Vivo (RabbitMQ)", construirVistaNotificaciones());

        // Agrega ambas pestañas al contenedor TabPane en orden de presentación.
        panelPestanas.getTabs().addAll(pestanaVehiculos, pestanaNotificaciones);

        // Envuelve el TabPane en un contenedor raíz vertical.
        VBox raiz = new VBox(panelPestanas);
        // Permite que el panel de pestañas se expanda ocupando todo el alto disponible de la ventana.
        VBox.setVgrow(panelPestanas, Priority.ALWAYS);

        // Crea la escena principal con dimensiones iniciales de 1080x650 píxeles.
        Scene escena = new Scene(raiz, 1080, 650);
        // Define el título institucional de la ventana de escritorio.
        ventana.setTitle("UTA SOA APE 04 - Cliente de Escritorio JavaFX");
        // Asocia la escena construida a la ventana principal (Stage).
        ventana.setScene(escena);

        // Iniciar temporizador de sincronización automática de notificaciones cada 3 segundos [Actividad 9].
        iniciarAutoSync();

        // Registra un manejador de cierre para detener limpiamente el Timeline y liberar recursos al cerrar la app.
        ventana.setOnCloseRequest(e -> {
            // Verifica que el temporizador haya sido inicializado antes de detenerlo.
            if (temporizadorSync != null) {
                // Detiene el sondeo periódico de notificaciones.
                temporizadorSync.stop();
            }
        });

        // Hace visible la ventana principal al usuario.
        ventana.show();
    }

    // Construye el panel gráfico completo de la pestaña "Catálogo de Vehículos" [Actividad 9].
    private VBox construirVistaVehiculos() {
        // Crea el encabezado descriptivo del módulo indicando conexión vía NGINX (:8090).
        Label titulo = new Label("Catálogo de Vehículos - Gateway NGINX (Puerto 8090)");
        // Aplica tipografía destacada en azul oscuro mediante estilos en línea de JavaFX.
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a365d;");

        // Define el texto guía del campo de contraseña para ingresar el JWT sin el prefijo Bearer [Actividad 9].
        txtToken.setPromptText("Ingrese su token JWT aquí (sin Bearer)");
        // Expande el campo JWT al ancho máximo del contenedor.
        txtToken.setMaxWidth(Double.MAX_VALUE);

        // Asocia el evento de clic del botón a la ejecución asíncrona de consultarVehiculos().
        btnConsultar.setOnAction(evento -> consultarVehiculos());

        // Agrupa horizontalmente el botón de consulta y la etiqueta de estado HTTP con 12px de separación.
        HBox acciones = new HBox(12, btnConsultar, lblEstadoVehiculos);
        // Alinea verticalmente al centro los controles de la barra de acciones.
        acciones.setAlignment(Pos.CENTER_LEFT);
        // Habilita salto de línea automático en mensajes largos de estado o error.
        lblEstadoVehiculos.setWrapText(true);

        // Configura cada columna de la tabla enlazándola por referencia de método a los campos del record Vehiculo [Actividad 1 / Actividad 9].
        // Columna de identificador único del vehículo.
        agregarColumnaVehiculo("ID", Vehiculo::idVehiculo, 60);
        // Columna de marca comercial.
        agregarColumnaVehiculo("Marca", Vehiculo::marca, 110);
        // Columna de modelo del vehículo.
        agregarColumnaVehiculo("Modelo", Vehiculo::modelo, 120);
        // Columna de año de fabricación.
        agregarColumnaVehiculo("Año", Vehiculo::año, 70);
        // Columna de color exterior.
        agregarColumnaVehiculo("Color", Vehiculo::color, 90);
        // Columna de placa vehicular.
        agregarColumnaVehiculo("Placa", Vehiculo::placa, 100);
        // Columna de número de chasis (VIN).
        agregarColumnaVehiculo("Chasis", Vehiculo::chasis, 150);
        // Columna de estado comercial (DISPONIBLE / VENDIDO tras ventas en la PWA) [Actividad 10 - P27].
        agregarColumnaVehiculo("Estado", Vehiculo::estado, 100);
        // Columna de precio monetario.
        agregarColumnaVehiculo("Precio", Vehiculo::precio, 90);

        // Establece el mensaje informativo mostrado cuando la tabla todavía no tiene filas cargadas.
        tablaVehiculos.setPlaceholder(new Label("No hay vehículos para mostrar. Presione 'Consultar Vehículos'."));
        // Permite que la tabla de vehículos crezca ocupando todo el espacio vertical remanente.
        VBox.setVgrow(tablaVehiculos, Priority.ALWAYS);

        // Ensambla todos los elementos de la pestaña en un contenedor vertical con espaciado de 12px.
        VBox contenedor = new VBox(12, titulo, txtToken, acciones, lblTotalVehiculos, tablaVehiculos);
        // Aplica un margen interno uniforme de 18px alrededor del contenido.
        contenedor.setPadding(new Insets(18));
        // Asigna un fondo gris claro suave a la vista.
        contenedor.setStyle("-fx-background-color: #f7fafc;");
        // Retorna el panel ensamblado para insertarlo en la primera pestaña.
        return contenedor;
    }

    // Construye el panel gráfico completo de la pestaña "Notificaciones en Vivo (RabbitMQ)" [Actividad 9 / Actividad 10 - P27].
    private VBox construirVistaNotificaciones() {
        // Crea el encabezado principal de la vista de supervisión de eventos.
        Label titulo = new Label("Supervisión de Eventos y Notificaciones de Ventas (RabbitMQ)");
        // Estiliza el título principal de la segunda pestaña.
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c5282;");

        // Muestra una nota aclaratoria sobre el origen de los eventos en la cola 'notificaciones.ventas' (consumidos vía API, no directo a RabbitMQ) [Actividad 9].
        Label explicacion = new Label("Recibe asíncronamente notificaciones de ventas publicadas a la cola 'notificaciones.ventas'.");
        // Estiliza el texto explicativo con tamaño secundario y tono gris pizarra.
        explicacion.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        // Activa por defecto el sondeo automático periódico cada 3 segundos [Actividad 9].
        chkAutoRefresh.setSelected(true);
        // Vincula el cambio de estado de la casilla para reanudar o pausar el Timeline [Actividad 9].
        chkAutoRefresh.setOnAction(e -> {
            // Si el usuario activa el auto-refresco, reanuda el temporizador si existe.
            if (chkAutoRefresh.isSelected()) {
                if (temporizadorSync != null) temporizadorSync.play();
            } else {
                // Si el usuario desmarca la casilla, pausa temporalmente las consultas periódicas.
                if (temporizadorSync != null) temporizadorSync.pause();
            }
        });

        // Permite disparar manualmente un sondeo inmediato hacia GET /api/notificaciones [Actividad 9].
        btnActualizarNotificaciones.setOnAction(e -> consultarNotificaciones());

        // Organiza horizontalmente el botón manual, la casilla de sondeo y la hora de última sincronización.
        HBox barraAcciones = new HBox(15, btnActualizarNotificaciones, chkAutoRefresh, lblUltimaSync);
        // Centra verticalmente los controles dentro de la barra de acciones.
        barraAcciones.setAlignment(Pos.CENTER_LEFT);

        // Estilo inicial neutro del banner de estado de conexión antes de recibir la primera respuesta HTTP [Actividad 9].
        lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #edf2f7; -fx-background-radius: 6;");
        // Expande el banner al ancho completo disponible.
        lblEstadoConexionNotif.setMaxWidth(Double.MAX_VALUE);
        // Permite múltiples líneas en mensajes de diagnóstico detallados (por ejemplo HTTP 502/504) [Actividad 11].
        lblEstadoConexionNotif.setWrapText(true);

        // Configura las 10 columnas de la tabla enlazándolas a NotificacionVentaDto [Actividad 1 / Actividad 9 / Actividad 10 - P27].
        // Columna del ID interno del registro de notificación.
        agregarColumnaNotificacion("ID", NotificacionVentaDto::id, 45);
        // Columna del identificador del evento RabbitMQ.
        agregarColumnaNotificacion("Evento ID", NotificacionVentaDto::eventoId, 170);
        // Columna del tipo de evento recibido.
        agregarColumnaNotificacion("Tipo", NotificacionVentaDto::tipo, 70);
        // Columna del ID de la venta generada desde la PWA [Actividad 10 - P27].
        agregarColumnaNotificacion("ID Venta", NotificacionVentaDto::ventaId, 65);
        // Columna del ID del vehículo involucrado en la venta [Actividad 10 - P27].
        agregarColumnaNotificacion("ID Veh.", NotificacionVentaDto::vehiculoId, 60);
        // Columna del ID del comprador registrado en la venta [Actividad 10 - P27].
        agregarColumnaNotificacion("ID Comp.", NotificacionVentaDto::compradorId, 65);
        // Columna del UUID de operación distribuida generado por la PWA [Actividad 10 - P27].
        agregarColumnaNotificacion("Operación ID", NotificacionVentaDto::operacionId, 160);
        // Columna del mensaje descriptivo de la notificación.
        agregarColumnaNotificacion("Mensaje", NotificacionVentaDto::mensaje, 220);
        // Columna del estado del procesamiento de la notificación.
        agregarColumnaNotificacion("Estado", NotificacionVentaDto::estado, 85);
        // Columna de fecha/hora mapeada desde fechaRegistro/fechaUTC [Actividad 1 / Actividad 9].
        agregarColumnaNotificacion("Fecha", NotificacionVentaDto::fecha, 135);

        // Mensaje inicial en tabla mientras completa el primer sondeo hacia NGINX (:8090).
        tablaNotificaciones.setPlaceholder(new Label("Consultando historial de notificaciones vía NGINX (:8090)..."));
        // Expande verticalmente la tabla de notificaciones al cambiar el tamaño de la ventana.
        VBox.setVgrow(tablaNotificaciones, Priority.ALWAYS);

        // Ensambla todos los controles de la segunda pestaña en un contenedor vertical.
        VBox contenedor = new VBox(10, titulo, explicacion, barraAcciones, lblEstadoConexionNotif, lblTotalNotificaciones, tablaNotificaciones);
        // Aplica padding uniforme de 18px.
        contenedor.setPadding(new Insets(18));
        // Aplica el mismo fondo claro coherente con la pestaña del catálogo.
        contenedor.setStyle("-fx-background-color: #f7fafc;");
        // Retorna la vista armada de notificaciones.
        return contenedor;
    }

    // Inicializa la carga inmediata y programa el sondeo automático Timeline cada 3 segundos hacia GET /api/notificaciones [Actividad 9].
    private void iniciarAutoSync() {
        // Realiza una primera consulta inmediata al abrir la aplicación sin esperar los primeros 3 segundos.
        consultarNotificaciones();
        // Crea un Timeline que dispara consultarNotificaciones() cada 3 segundos exactos [Actividad 9].
        temporizadorSync = new Timeline(new KeyFrame(Duration.seconds(3), e -> consultarNotificaciones()));
        // Configura la repetición indefinida mientras la ventana permanezca abierta y el checkbox activo.
        temporizadorSync.setCycleCount(Timeline.INDEFINITE);
        // Arranca inmediatamente la ejecución del ciclo de sondeo automático [Actividad 9].
        temporizadorSync.play();
    }

    // Orquesta la consulta autenticada del catálogo de vehículos (GET /api/vehiculos con JWT) vía NGINX (:8090) [Actividad 9 / Actividad 11].
    private void consultarVehiculos() {
        // Lee el JWT ingresado por el usuario eliminando espacios en blanco en los extremos.
        String jwt = txtToken.getText().trim();
        // Si el usuario pegó por error el prefijo "Bearer " (sin distinguir mayúsculas), lo remueve limpiamente.
        if (jwt.regionMatches(true, 0, "Bearer ", 0, 7)) {
            jwt = jwt.substring(7).trim();
        }

        // Deshabilita temporalmente el botón para evitar solicitudes duplicadas simultáneas.
        btnConsultar.setDisable(true);
        // Informa visualmente al usuario que la solicitud está en tránsito hacia NGINX.
        mostrarEstadoVehiculos("Consultando catálogo a través de NGINX...", false);

        // Lanza la petición HTTP asíncrona y procesa tanto el resultado como cualquier excepción de red [Actividad 9].
        api.consultarVehiculos(jwt).whenComplete((resultado, error) -> {
            // Despacha toda modificación de controles visuales al hilo oficial de JavaFX mediante Platform.runLater [Actividad 9].
            Platform.runLater(() -> {
                // Rehabilita el botón de consulta independientemente del resultado obtenido.
                btnConsultar.setDisable(false);
                // Si ocurrió un error de conexión, timeout o parseo JSON, informa la falla de enlace con NGINX (:8090) [Actividad 11].
                if (error != null) {
                    mostrarEstadoVehiculos("Error de conexión: Verifique el Gateway NGINX (8090).", true);
                    return;
                }

                // Evalúa los códigos HTTP reales devueltos por NGINX/Spring Boot [Actividad 11].
                if (resultado.codigoHttp() == 401) {
                    // HTTP 401 Unauthorized: token ausente, malformado o vencido [Actividad 11].
                    mostrarEstadoVehiculos("HTTP 401: Token JWT no válido o expirado.", true);
                } else if (resultado.codigoHttp() == 403) {
                    // HTTP 403 Forbidden: token válido pero sin permisos/alcances suficientes [Actividad 11].
                    mostrarEstadoVehiculos("HTTP 403: Permisos insuficientes para consultar vehículos.", true);
                } else if (resultado.codigoHttp() == 200) {
                    // HTTP 200 OK: reemplaza el contenido de la tabla con el catálogo real (reflejando DISPONIBLE/VENDIDO) [Actividad 9 / Actividad 10 - P27].
                    tablaVehiculos.getItems().setAll(resultado.vehiculos());
                    // Actualiza el contador visible con el número exacto de vehículos retornados.
                    lblTotalVehiculos.setText("Total de Vehículos: " + resultado.vehiculos().size());
                    // Muestra confirmación visual de consulta exitosa vía NGINX.
                    mostrarEstadoVehiculos("Consulta completada con éxito vía NGINX (HTTP 200).", false);
                } else {
                    // Informa cualquier otro código HTTP inesperado (ej. 500, 502, 503, 504) sin ocultar el fallo [Actividad 11].
                    mostrarEstadoVehiculos("Respuesta HTTP inesperada: " + resultado.codigoHttp(), true);
                }
            });
        });
    }

    // Ejecuta un ciclo de sondeo hacia GET /api/notificaciones y actualiza los 3 estados visuales (ROJO, AZUL, VERDE) con Platform.runLater [Actividad 9 / Actividad 10 - P27 / Actividad 11].
    private void consultarNotificaciones() {
        // Invoca asíncronamente al cliente HTTP para obtener ResultadoNotificaciones sin bloquear la UI [Actividad 9].
        api.consultarNotificaciones().whenComplete((resultado, error) -> {
            // Encola la actualización de etiquetas y tabla en el hilo gráfico de JavaFX [Actividad 9].
            Platform.runLater(() -> {
                // Registra la hora actual exacta en formato HH:mm:ss para auditoría del sondeo.
                String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                // ESTADO VISUAL ROJO (Caso 1): fallo de transporte/red cuando NGINX (:8090) no responde o está caído [Actividad 9 / Actividad 11].
                if (error != null || resultado == null) {
                    // Muestra mensaje claro de desconexión con el Gateway NGINX.
                    lblEstadoConexionNotif.setText("FALLO DE CONEXION: No se pudo contactar al Gateway NGINX (http://127.0.0.1:8090/api/notificaciones).");
                    // Aplica color de texto rojo carmesí para alertar del fallo.
                    lblEstadoConexionNotif.setTextFill(Color.CRIMSON);
                    // Aplica fondo y borde rojizos al banner de estado.
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #fff5f5; -fx-border-color: #feb2b2; -fx-background-radius: 6; -fx-border-radius: 6;");
                    // Actualiza la hora del último intento fallido.
                    lblUltimaSync.setText("Último intento fallido: " + hora);
                    // Reemplaza el placeholder de la tabla con un aviso rojo de error de servicio.
                    Label placeholderError = new Label("ERROR DE SERVICIO: Conexión rechazada con NGINX o notificaciones-service.");
                    placeholderError.setTextFill(Color.CRIMSON);
                    tablaNotificaciones.setPlaceholder(placeholderError);
                    return;
                }

                // ESTADO VISUAL ROJO (Caso 2): respuesta HTTP != 200 (ej. 502 Bad Gateway / 504 Gateway Timeout al detener notificaciones-service) o JSON corrupto [Actividad 9 / Actividad 11].
                if (resultado.codigoHttp() != 200 || resultado.errorDetalle() != null) {
                    // Muestra el detalle exacto del error HTTP generado por VehiculosApiClient [Actividad 11].
                    lblEstadoConexionNotif.setText("FALLO DE SERVICIO — " + resultado.errorDetalle());
                    // Colorea el texto de alerta en rojo carmesí.
                    lblEstadoConexionNotif.setTextFill(Color.CRIMSON);
                    // Aplica estilo visual rojo al banner para diferenciarlo claramente de una tabla vacía saludable [Actividad 9 / Actividad 11].
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #fff5f5; -fx-border-color: #feb2b2; -fx-background-radius: 6; -fx-border-radius: 6;");
                    // Registra la hora del fallo HTTP.
                    lblUltimaSync.setText("Último intento fallido: " + hora);
                    // Actualiza el placeholder de la tabla detallando el código HTTP fallido (502/503/504) [Actividad 11].
                    Label placeholderHttp = new Label("FALLO DE SERVICIO (" + resultado.codigoHttp() + "): " + resultado.errorDetalle());
                    placeholderHttp.setTextFill(Color.CRIMSON);
                    tablaNotificaciones.setPlaceholder(placeholderHttp);
                    return;
                }

                // Extrae la lista válida de notificaciones obtenida con HTTP 200 OK [Actividad 9].
                var lista = resultado.notificaciones();
                // Actualiza las filas de la tabla reflejando automáticamente ventaId, vehiculoId, compradorId y operacionId de la PWA [Actividad 10 - P27].
                tablaNotificaciones.getItems().setAll(lista);
                // Actualiza el contador total de notificaciones recibidas.
                lblTotalNotificaciones.setText("Notificaciones recibidas: " + lista.size());
                // Registra la hora exacta de la sincronización exitosa.
                lblUltimaSync.setText("Última sincronización: " + hora);

                // Diferencia entre pantalla vacía saludable (AZUL) y operación conectada con registros (VERDE) [Actividad 9].
                if (lista.isEmpty()) {
                    // ESTADO VISUAL AZUL: HTTP 200 con arreglo vacío [] indicando servicio saludable sin ventas aún [Actividad 9].
                    lblEstadoConexionNotif.setText("CONECTADO (HTTP 200) — Servicio operativo, sin notificaciones de ventas registradas todavía.");
                    // Aplica color azul oscuro al texto del banner.
                    lblEstadoConexionNotif.setTextFill(Color.DARKBLUE);
                    // Aplica fondo celeste y borde azul para señalar conectividad saludable sin datos.
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #ebf8ff; -fx-border-color: #90cdf4; -fx-background-radius: 6; -fx-border-radius: 6;");
                    // Muestra placeholder azul confirmando servicio conectado con pantalla vacía [Actividad 9].
                    Label placeholderVacio = new Label("Servicio conectado (HTTP 200). Pantalla vacía: aún no existen notificaciones de ventas.");
                    placeholderVacio.setTextFill(Color.DARKBLUE);
                    tablaNotificaciones.setPlaceholder(placeholderVacio);
                } else {
                    // ESTADO VISUAL VERDE: HTTP 200 con notificaciones reales de ventas disponibles en historial [Actividad 9 / Actividad 10 - P27].
                    lblEstadoConexionNotif.setText("CONECTADO (HTTP 200) — Sondeo activo vía NGINX (:8090). " + lista.size() + " notificación(es) en historial.");
                    // Aplica color verde oscuro al texto del banner.
                    lblEstadoConexionNotif.setTextFill(Color.DARKGREEN);
                    // Aplica fondo verde menta y borde verde para confirmar recepción activa de notificaciones.
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #f0fff4; -fx-border-color: #9ae6b4; -fx-background-radius: 6; -fx-border-radius: 6;");
                }
            });
        });
    }

    // Actualiza el mensaje y el color semántico (rojo para error, azul oscuro para estado normal) en la pestaña de vehículos [Actividad 9 / Actividad 11].
    private void mostrarEstadoVehiculos(String texto, boolean error) {
        // Establece el texto descriptivo del estado actual de la consulta.
        lblEstadoVehiculos.setText(texto);
        // Colorea en rojo si es error (401, 403, red) o en azul oscuro si es operación normal/exitosa.
        lblEstadoVehiculos.setTextFill(error ? Color.RED : Color.DARKBLUE);
    }

    // Método utilitario genérico que crea y añade una columna tipada a la tabla de vehículos [Actividad 9].
    private <T> void agregarColumnaVehiculo(String titulo, Function<Vehiculo, T> valor, double ancho) {
        // Instancia la columna con su encabezado visible.
        TableColumn<Vehiculo, T> columna = new TableColumn<>(titulo);
        // Define cómo extraer el atributo del record Vehiculo envolviéndolo en un ReadOnlyObjectWrapper observable.
        columna.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(valor.apply(fila.getValue())));
        // Asigna el ancho preferido en píxeles para una lectura ordenada.
        columna.setPrefWidth(ancho);
        // Registra la columna en tablaVehiculos.
        tablaVehiculos.getColumns().add(columna);
    }

    // Método utilitario genérico que crea y añade una columna tipada a la tabla de notificaciones [Actividad 9 / Actividad 10 - P27].
    private <T> void agregarColumnaNotificacion(String titulo, Function<NotificacionVentaDto, T> valor, double ancho) {
        // Instancia la columna con su encabezado visible.
        TableColumn<NotificacionVentaDto, T> columna = new TableColumn<>(titulo);
        // Define la fábrica de valores de celda aplicando la función extractora sobre cada NotificacionVentaDto.
        columna.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(valor.apply(fila.getValue())));
        // Configura el ancho preferido en píxeles de la columna.
        columna.setPrefWidth(ancho);
        // Registra la columna en tablaNotificaciones.
        tablaNotificaciones.getColumns().add(columna);
    }
}
