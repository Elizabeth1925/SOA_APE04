package ec.edu.com;

// Importaciones de Java estándar para funciones lambdas y manejo de datos
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

// Importaciones de JavaFX para propiedades observables en tablas
import javafx.beans.property.ReadOnlyObjectWrapper;

// Importaciones base del ciclo de vida y despacho de hilos en JavaFX
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.util.Duration;

// Importaciones de diseño (layout), alineación y márgenes
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Importaciones de controles visuales de JavaFX
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// Importación de colores oficial de JavaFX
import javafx.scene.paint.Color;

// Importación de la ventana principal
import javafx.stage.Stage;

/**
 * Ventana principal de la aplicación JavaFX (SOA APE 04).
 * Permite la consulta de vehículos balanceados vía NGINX (3:1) y la supervisión
 * en tiempo real de notificaciones de ventas asíncronas emitidas por RabbitMQ.
 */
public final class VehiculosApp extends Application {

    private final VehiculosApiClient api = new VehiculosApiClient();

    // Controles pestaña de Vehículos
    private final PasswordField txtToken = new PasswordField();
    private final Button btnConsultar = new Button("Consultar Vehículos");
    private final Label lblEstadoVehiculos = new Label("Listo para consultar vía NGINX");
    private final Label lblTotalVehiculos = new Label("Total de Vehículos: 0");
    private final TableView<Vehiculo> tablaVehiculos = new TableView<>();

    // Controles pestaña de Notificaciones RabbitMQ
    private final TableView<NotificacionVentaDto> tablaNotificaciones = new TableView<>();
    private final Button btnActualizarNotificaciones = new Button("Actualizar Notificaciones");
    private final CheckBox chkAutoRefresh = new CheckBox("Sondeo automático (cada 3s)");
    private final Label lblEstadoConexionNotif = new Label("Estado de conexión: Iniciando enlace con NGINX (:8090)...");
    private final Label lblTotalNotificaciones = new Label("Notificaciones recibidas: 0");
    private final Label lblUltimaSync = new Label("Última sincronización: —");

    private Timeline temporizadorSync;

    @Override
    public void start(Stage ventana) {
        TabPane panelPestanas = new TabPane();
        panelPestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // PESTAÑA 1: CATÁLOGO DE VEHÍCULOS
        Tab pestanaVehiculos = new Tab("🚗 Catálogo de Vehículos", construirVistaVehiculos());

        // PESTAÑA 2: NOTIFICACIONES DE VENTAS RABBITMQ
        Tab pestanaNotificaciones = new Tab("🔔 Notificaciones en Vivo (RabbitMQ)", construirVistaNotificaciones());

        panelPestanas.getTabs().addAll(pestanaVehiculos, pestanaNotificaciones);

        VBox raiz = new VBox(panelPestanas);
        VBox.setVgrow(panelPestanas, Priority.ALWAYS);

        Scene escena = new Scene(raiz, 1080, 650);
        ventana.setTitle("UTA SOA APE 04 - Cliente de Escritorio JavaFX");
        ventana.setScene(escena);

        // Iniciar temporizador de sincronización automática de notificaciones
        iniciarAutoSync();

        ventana.setOnCloseRequest(e -> {
            if (temporizadorSync != null) {
                temporizadorSync.stop();
            }
        });

        ventana.show();
    }

    private VBox construirVistaVehiculos() {
        Label titulo = new Label("Catálogo de Vehículos - Gateway NGINX (Puerto 8090)");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1a365d;");

        txtToken.setPromptText("Ingrese su token JWT aquí (sin Bearer)");
        txtToken.setMaxWidth(Double.MAX_VALUE);

        btnConsultar.setOnAction(evento -> consultarVehiculos());

        HBox acciones = new HBox(12, btnConsultar, lblEstadoVehiculos);
        acciones.setAlignment(Pos.CENTER_LEFT);
        lblEstadoVehiculos.setWrapText(true);

        agregarColumnaVehiculo("ID", Vehiculo::idVehiculo, 60);
        agregarColumnaVehiculo("Marca", Vehiculo::marca, 110);
        agregarColumnaVehiculo("Modelo", Vehiculo::modelo, 120);
        agregarColumnaVehiculo("Año", Vehiculo::año, 70);
        agregarColumnaVehiculo("Color", Vehiculo::color, 90);
        agregarColumnaVehiculo("Placa", Vehiculo::placa, 100);
        agregarColumnaVehiculo("Chasis", Vehiculo::chasis, 150);
        agregarColumnaVehiculo("Estado", Vehiculo::estado, 100);
        agregarColumnaVehiculo("Precio", Vehiculo::precio, 90);

        tablaVehiculos.setPlaceholder(new Label("No hay vehículos para mostrar. Presione 'Consultar Vehículos'."));
        VBox.setVgrow(tablaVehiculos, Priority.ALWAYS);

        VBox contenedor = new VBox(12, titulo, txtToken, acciones, lblTotalVehiculos, tablaVehiculos);
        contenedor.setPadding(new Insets(18));
        contenedor.setStyle("-fx-background-color: #f7fafc;");
        return contenedor;
    }

    private VBox construirVistaNotificaciones() {
        Label titulo = new Label("Supervisión de Eventos y Notificaciones de Ventas (RabbitMQ)");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c5282;");

        Label explicacion = new Label("Recibe asíncronamente notificaciones de ventas publicadas a la cola 'notificaciones.ventas'.");
        explicacion.setStyle("-fx-font-size: 12px; -fx-text-fill: #4a5568;");

        chkAutoRefresh.setSelected(true);
        chkAutoRefresh.setOnAction(e -> {
            if (chkAutoRefresh.isSelected()) {
                if (temporizadorSync != null) temporizadorSync.play();
            } else {
                if (temporizadorSync != null) temporizadorSync.pause();
            }
        });

        btnActualizarNotificaciones.setOnAction(e -> consultarNotificaciones());

        HBox barraAcciones = new HBox(15, btnActualizarNotificaciones, chkAutoRefresh, lblUltimaSync);
        barraAcciones.setAlignment(Pos.CENTER_LEFT);

        lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #edf2f7; -fx-background-radius: 6;");
        lblEstadoConexionNotif.setMaxWidth(Double.MAX_VALUE);
        lblEstadoConexionNotif.setWrapText(true);

        agregarColumnaNotificacion("ID", NotificacionVentaDto::id, 45);
        agregarColumnaNotificacion("Evento ID", NotificacionVentaDto::eventoId, 170);
        agregarColumnaNotificacion("Tipo", NotificacionVentaDto::tipo, 70);
        agregarColumnaNotificacion("ID Venta", NotificacionVentaDto::ventaId, 65);
        agregarColumnaNotificacion("ID Veh.", NotificacionVentaDto::vehiculoId, 60);
        agregarColumnaNotificacion("ID Comp.", NotificacionVentaDto::compradorId, 65);
        agregarColumnaNotificacion("Operación ID", NotificacionVentaDto::operacionId, 160);
        agregarColumnaNotificacion("Mensaje", NotificacionVentaDto::mensaje, 220);
        agregarColumnaNotificacion("Estado", NotificacionVentaDto::estado, 85);
        agregarColumnaNotificacion("Fecha", NotificacionVentaDto::fecha, 135);

        tablaNotificaciones.setPlaceholder(new Label("Consultando historial de notificaciones vía NGINX (:8090)..."));
        VBox.setVgrow(tablaNotificaciones, Priority.ALWAYS);

        VBox contenedor = new VBox(10, titulo, explicacion, barraAcciones, lblEstadoConexionNotif, lblTotalNotificaciones, tablaNotificaciones);
        contenedor.setPadding(new Insets(18));
        contenedor.setStyle("-fx-background-color: #f7fafc;");
        return contenedor;
    }

    private void iniciarAutoSync() {
        consultarNotificaciones();
        temporizadorSync = new Timeline(new KeyFrame(Duration.seconds(3), e -> consultarNotificaciones()));
        temporizadorSync.setCycleCount(Timeline.INDEFINITE);
        temporizadorSync.play();
    }

    private void consultarVehiculos() {
        String jwt = txtToken.getText().trim();
        if (jwt.regionMatches(true, 0, "Bearer ", 0, 7)) {
            jwt = jwt.substring(7).trim();
        }

        btnConsultar.setDisable(true);
        mostrarEstadoVehiculos("Consultando catálogo a través de NGINX...", false);

        api.consultarVehiculos(jwt).whenComplete((resultado, error) -> {
            Platform.runLater(() -> {
                btnConsultar.setDisable(false);
                if (error != null) {
                    mostrarEstadoVehiculos("Error de conexión: Verifique el Gateway NGINX (8090).", true);
                    return;
                }

                if (resultado.codigoHttp() == 401) {
                    mostrarEstadoVehiculos("HTTP 401: Token JWT no válido o expirado.", true);
                } else if (resultado.codigoHttp() == 403) {
                    mostrarEstadoVehiculos("HTTP 403: Permisos insuficientes para consultar vehículos.", true);
                } else if (resultado.codigoHttp() == 200) {
                    tablaVehiculos.getItems().setAll(resultado.vehiculos());
                    lblTotalVehiculos.setText("Total de Vehículos: " + resultado.vehiculos().size());
                    mostrarEstadoVehiculos("Consulta completada con éxito vía NGINX (HTTP 200).", false);
                } else {
                    mostrarEstadoVehiculos("Respuesta HTTP inesperada: " + resultado.codigoHttp(), true);
                }
            });
        });
    }

    private void consultarNotificaciones() {
        api.consultarNotificaciones().whenComplete((resultado, error) -> {
            Platform.runLater(() -> {
                String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                if (error != null || resultado == null) {
                    lblEstadoConexionNotif.setText("FALLO DE CONEXION: No se pudo contactar al Gateway NGINX (http://127.0.0.1:8090/api/notificaciones).");
                    lblEstadoConexionNotif.setTextFill(Color.CRIMSON);
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #fff5f5; -fx-border-color: #feb2b2; -fx-background-radius: 6; -fx-border-radius: 6;");
                    lblUltimaSync.setText("Último intento fallido: " + hora);
                    Label placeholderError = new Label("ERROR DE SERVICIO: Conexión rechazada con NGINX o notificaciones-service.");
                    placeholderError.setTextFill(Color.CRIMSON);
                    tablaNotificaciones.setPlaceholder(placeholderError);
                    return;
                }

                if (resultado.codigoHttp() != 200 || resultado.errorDetalle() != null) {
                    lblEstadoConexionNotif.setText("FALLO DE SERVICIO — " + resultado.errorDetalle());
                    lblEstadoConexionNotif.setTextFill(Color.CRIMSON);
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #fff5f5; -fx-border-color: #feb2b2; -fx-background-radius: 6; -fx-border-radius: 6;");
                    lblUltimaSync.setText("Último intento fallido: " + hora);
                    Label placeholderHttp = new Label("FALLO DE SERVICIO (" + resultado.codigoHttp() + "): " + resultado.errorDetalle());
                    placeholderHttp.setTextFill(Color.CRIMSON);
                    tablaNotificaciones.setPlaceholder(placeholderHttp);
                    return;
                }

                var lista = resultado.notificaciones();
                tablaNotificaciones.getItems().setAll(lista);
                lblTotalNotificaciones.setText("Notificaciones recibidas: " + lista.size());
                lblUltimaSync.setText("Última sincronización: " + hora);

                if (lista.isEmpty()) {
                    lblEstadoConexionNotif.setText("CONECTADO (HTTP 200) — Servicio operativo, sin notificaciones de ventas registradas todavía.");
                    lblEstadoConexionNotif.setTextFill(Color.DARKBLUE);
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #ebf8ff; -fx-border-color: #90cdf4; -fx-background-radius: 6; -fx-border-radius: 6;");
                    Label placeholderVacio = new Label("Servicio conectado (HTTP 200). Pantalla vacía: aún no existen notificaciones de ventas.");
                    placeholderVacio.setTextFill(Color.DARKBLUE);
                    tablaNotificaciones.setPlaceholder(placeholderVacio);
                } else {
                    lblEstadoConexionNotif.setText("CONECTADO (HTTP 200) — Sondeo activo vía NGINX (:8090). " + lista.size() + " notificación(es) en historial.");
                    lblEstadoConexionNotif.setTextFill(Color.DARKGREEN);
                    lblEstadoConexionNotif.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 10 6 10; -fx-background-color: #f0fff4; -fx-border-color: #9ae6b4; -fx-background-radius: 6; -fx-border-radius: 6;");
                }
            });
        });
    }

    private void mostrarEstadoVehiculos(String texto, boolean error) {
        lblEstadoVehiculos.setText(texto);
        lblEstadoVehiculos.setTextFill(error ? Color.RED : Color.DARKBLUE);
    }

    private <T> void agregarColumnaVehiculo(String titulo, Function<Vehiculo, T> valor, double ancho) {
        TableColumn<Vehiculo, T> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(valor.apply(fila.getValue())));
        columna.setPrefWidth(ancho);
        tablaVehiculos.getColumns().add(columna);
    }

    private <T> void agregarColumnaNotificacion(String titulo, Function<NotificacionVentaDto, T> valor, double ancho) {
        TableColumn<NotificacionVentaDto, T> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(valor.apply(fila.getValue())));
        columna.setPrefWidth(ancho);
        tablaNotificaciones.getColumns().add(columna);
    }
}
