package ec.edu.com;

// Importaciones de Java estándar para funciones lambdas y manejo de datos
import java.util.function.Function;

// Importaciones de JavaFX para propiedades observables en tablas
import javafx.beans.property.ReadOnlyObjectWrapper;

// Importaciones base del ciclo de vida y despacho de hilos en JavaFX
import javafx.application.Application;
import javafx.application.Platform;

// Importaciones de diseño (layout), alineación y márgenes
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Importaciones de controles visuales de JavaFX
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

// Importación de colores oficial de JavaFX (reemplaza java.awt.Color)
import javafx.scene.paint.Color;

// Importación de la ventana principal
import javafx.stage.Stage;

/**
 * Ventana principal de la aplicación JavaFX encargada de la consulta y visualización de vehículos.
 * Equivale funcionalmente al FormPrincipal de Windows Forms.
 */
public final class VehiculosApp extends Application {

    // Instancia del cliente HTTP para consumir el microservicio REST
    private final VehiculosApiClient api = new VehiculosApiClient();

    // Campo de contraseña para ingresar el token JWT de forma segura
    private final PasswordField txtToken = new PasswordField();

    // Botón para disparar la consulta al servidor
    private final Button btnConsultar = new Button("Consultar");

    // Etiqueta para informar el estado actual de la petición (cargando, errores, éxito)
    private final Label lblEstado = new Label("Listo");

    // Etiqueta para mostrar el conteo total de vehículos obtenidos
    private final Label lblTotal = new Label("Total de Vehiculos: 0");

    // Tabla visual parametrizada con el modelo record Vehiculo
    private final TableView<Vehiculo> tabla = new TableView<>();

    @Override
    public void start(Stage ventana) {
        // Etiqueta de título superior de la ventana
        Label titulo = new Label("Ventana Vehiculos Registrados");

        // Texto informativo de ayuda dentro del campo de texto
        txtToken.setPromptText("Ingrese su token JWT aqui");

        // Permitir que el campo de texto se expanda horizontalmente
        txtToken.setMaxWidth(Double.MAX_VALUE);

        // Ancho sugerido para el botón consultar
        btnConsultar.setMaxWidth(130);

        // Evento que se dispara al hacer clic en el botón o presionar Enter
        btnConsultar.setOnAction(evento -> consultar());

        // Contenedor horizontal que agrupa el botón y la etiqueta de estado en una sola línea
        HBox acciones = new HBox(12, btnConsultar, lblEstado);

        // Alineación vertical centrada hacia la izquierda dentro del HBox
        acciones.setAlignment(Pos.CENTER_LEFT);

        // Permitir que la etiqueta de estado ajuste el texto en varias líneas si es muy largo
        lblEstado.setWrapText(true);

        // Definición y agregación de cada columna de la tabla vinculada a su atributo en Vehiculo
        agregarColumna("ID", Vehiculo::idVehiculo, 65);
        agregarColumna("Marca", Vehiculo::marca, 119);
        agregarColumna("Modelo", Vehiculo::modelo, 135);
        // CORREGIDO: Se usa el método Vehiculo::año correspondiente al campo 'año' del record
        agregarColumna("Año", Vehiculo::año, 75);
        agregarColumna("Color", Vehiculo::color, 105);
        agregarColumna("Placa", Vehiculo::placa, 105);
        agregarColumna("Chasis", Vehiculo::chasis, 165);
        agregarColumna("Estado", Vehiculo::estado, 100);
        agregarColumna("Precio", Vehiculo::precio, 100);

        // Mensaje visible en la tabla cuando no existan registros cargados
        tabla.setPlaceholder(new Label("No hay vehiculos para mostrar"));

        // Hace que la tabla se expanda verticalmente ocupando el espacio disponible
        VBox.setVgrow(tabla, Priority.ALWAYS);

        // Contenedor vertical principal que organiza todos los componentes en orden
        // CORREGIDO: Se añade 'acciones' en vez de duplicar 'btnConsultar' y 'lblEstado'
        VBox contenido = new VBox(12, titulo, txtToken, acciones, lblTotal, tabla);

        // Márgenes internos de 22 píxeles alrededor de todo el contenido
        contenido.setPadding(new Insets(22));

        // Establece el color de fondo de toda la ventana a un tono lila elegante (#E8DCF0)
        contenido.setStyle("-fx-background-color: #e0bff7;");

        // Título de la ventana en la barra del sistema operativo
        ventana.setTitle("Consulta Vehiculos");

        // Asignación de la escena con un tamaño inicial de 1024x600 píxeles
        ventana.setScene(new Scene(contenido, 1024, 600));

        // Muestra la ventana en la pantalla del usuario
        ventana.show();
    }

    /**
     * Método encargado de validar el token, llamar al servicio web y actualizar la interfaz.
     */
    private void consultar() {
        // Obtiene el token ingresado eliminando espacios en blanco al inicio y al final
        String jwt = txtToken.getText().trim();

        // Si el usuario copió el token con la palabra "Bearer ", se la quitamos para no duplicarla
        if (jwt.regionMatches(true, 0, "Bearer ", 0, 7)) {
            jwt = jwt.substring(7).trim();
        }

        // Deshabilita el botón temporalmente para evitar peticiones duplicadas mientras carga
        btnConsultar.setDisable(true);

        // Muestra el mensaje informativo de que la consulta está en proceso
        mostrarEstado("Consultando vehiculos...", false);

        // Llamada asíncrona a la API REST a través del cliente HTTP
        api.consultarVehiculos(jwt).whenComplete((resultado, error) -> {
            // Regresa al hilo principal de JavaFX para modificar los componentes gráficos
            Platform.runLater(() -> {
                // Vuelve a habilitar el botón de consulta
                btnConsultar.setDisable(false);

                // Si ocurrió un error de red, conexión o tiempo de espera
                if (error != null) {
                    mostrarEstado("Error: No se pudo conectar con el servidor", true);
                    return;
                }

                // Evaluación del código de respuesta HTTP devuelto por la API
                if (resultado.codigoHttp() == 401) {
                    mostrarEstado("Http 401: Token no valido o expirado", true);
                } else if (resultado.codigoHttp() == 403) {
                    mostrarEstado("Http 403: No tienes permisos para consultar vehiculos", true);
                } else if (resultado.codigoHttp() == 200) {
                    // Carga la lista de vehículos en los ítems de la tabla
                    tabla.getItems().setAll(resultado.vehiculos());
                    // Actualiza la etiqueta con la cantidad de registros recibidos
                    lblTotal.setText("Total de Vehiculos: " + resultado.vehiculos().size());
                    // Muestra mensaje de éxito en color azul
                    mostrarEstado("Consulta exitosa. Se cargaron los vehiculos.", false);
                } else {
                    mostrarEstado("Respuesta HTTP inesperada: " + resultado.codigoHttp(), true);
                }
            });
        });
    }

    /**
     * Método auxiliar para actualizar el texto y color de la etiqueta de estado.
     * 
     * @param texto Mensaje a mostrar
     * @param error Si es true lo pinta de rojo, si es false de azul
     */
    private void mostrarEstado(String texto, boolean error) {
        // Establece el texto en la etiqueta
        lblEstado.setText(texto);
        // CORREGIDO: Uso de javafx.scene.paint.Color (Color.RED / Color.BLUE)
        lblEstado.setTextFill(error ? Color.RED : Color.BLUE);
    }

    /**
     * Método genérico reutilizable para crear, configurar y registrar una columna en la tabla.
     * 
     * @param <T> Tipo de dato de la columna (String, Integer, BigDecimal, etc.)
     * @param titulo Título de la cabecera de la columna
     * @param valor Función extractora del valor desde el objeto Vehiculo
     * @param ancho Ancho preferido en píxeles para la columna
     */
    private <T> void agregarColumna(String titulo, Function<Vehiculo, T> valor, double ancho) {
        // Instancia de la columna con su título
        TableColumn<Vehiculo, T> columna = new TableColumn<>(titulo);

        // CORREGIDO: Se usa ReadOnlyObjectWrapper para envolver el valor retornado por la Function
        columna.setCellValueFactory(fila -> new ReadOnlyObjectWrapper<>(valor.apply(fila.getValue())));

        // Asignación del ancho preferido
        columna.setPrefWidth(ancho);

        // Añade la columna configurada a la colección de columnas de la tabla
        tabla.getColumns().add(columna);
    }
}
