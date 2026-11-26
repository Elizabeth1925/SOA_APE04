// =================================================================================================
// Archivo: Form1.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Lógica de presentación (code-behind) del formulario principal Form1. Gestiona las
//            validaciones de entrada, la invocación asíncrona de los servicios HTTP hacia el
//            gateway NGINX (:8090), el enlace de resultados a los DataGridView y la retroalimentación
//            visual de códigos HTTP para las tres pestañas administrativas:
//              1) Vehículos: alta (POST /api/vehiculos), listado/filtro por marca (GET /api/vehiculos)
//                 y búsqueda individual (GET /api/vehiculos/{id}).
//              2) Compradores: alta (POST /api/compradores) y listado (GET /api/compradores).
//              3) Ventas (SOLO CONSULTA): listado (GET /api/ventas) y detalle (GET /api/ventas/{id}),
//                 prohibiendo explícitamente POST /api/ventas (exclusivo de la PWA móvil).
// Actividades APE 04 que implementa:
//   - [Actividad 1]: Mapeo y visualización tipada de contratos DTO (VehiculoDto, CrearVehiculoRequest,
//                    CompradorDto, CrearCompradorRequest y VentaDto).
//   - [Actividad 5]: Cliente administrativo Windows Forms (.NET 8 C#) completo con consumo unificado
//                    vía NGINX (:8090), envío de Bearer JWT, prohibición explícita de POST /api/ventas
//                    y manejo visual de códigos HTTP (400, 401, 403, 404, 409, 5xx).
//   - [Actividad 10 - P27]: Coherencia transversal de datos: altas administrativas visibles en React/PWA
//                           y auditoría post-venta del estado VENDIDO en catálogo y detalle de ventas.
//   - [Actividad 11]: Rechazo controlado ante 401/403 limpiando grillas/detalles (DataSource = null),
//                     sin desactivar seguridad ni mostrar datos ficticios.
// =================================================================================================

// [Actividad 1] Importa los contratos DTO (VehiculoDto, CompradorDto, VentaDto y requests de alta).
using ConsultaVehiculosForms.Models;
// [Actividad 5] Importa los clientes HTTP VehiculoApiClient y VentaApiClient configurados contra NGINX (:8090).
using ConsultaVehiculosForms.Services;

// [Actividad 5] Espacio de nombres raíz de la aplicación administrativa Windows Forms.
namespace ConsultaVehiculosForms
{
    // [Actividad 5] Clase parcial del formulario principal que concentra la lógica de eventos e integración HTTP.
    public partial class Form1 : Form
    {
        // [Actividad 5] Cliente HTTP inmutable para operaciones de vehículos contra Java Spring Boot vía NGINX (:8090).
        private readonly VehiculoApiClient _vehiculoClient = new();
        // [Actividad 5] Cliente HTTP inmutable para compradores (GET/POST) y ventas en SOLO CONSULTA (GET) contra FastAPI vía NGINX (:8090).
        private readonly VentaApiClient _ventaClient = new();

        /// <summary>
        /// [Actividad 5] Constructor principal del formulario Form1; inicializa todos los controles visuales diseñados.
        /// </summary>
        public Form1()
        {
            // [Actividad 5] Invoca el método del diseñador que construye cabecera JWT y las tres pestañas administrativas.
            InitializeComponent();
        }

        // ==========================================
        // 1. VEHÍCULOS (Java Spring Boot vía NGINX :8090)
        // [Actividad 5] [Actividad 10 - P27] [Actividad 11]
        // ==========================================

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Operación 1: Dar de alta un vehículo (POST /api/vehiculos vía NGINX :8090).
        /// El vehículo registrado aquí queda inmediatamente disponible en React y la PWA para el flujo transversal.
        /// </summary>
        private async void btnCrearVehiculo_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Obtiene el token Bearer JWT ingresado en la cabecera (si está vacío, el servidor responderá 401).
            string token = txtToken.Text.Trim();
            // [Actividad 5] Captura y recorta la marca del vehículo desde el campo de entrada.
            string marca = txtVMarca.Text.Trim();
            // [Actividad 5] Captura y recorta el modelo del vehículo desde el campo de entrada.
            string modelo = txtVModelo.Text.Trim();
            // [Actividad 5] Captura y recorta la placa única del vehículo.
            string placa = txtVPlaca.Text.Trim();
            // [Actividad 5] Captura y recorta el código de chasis (VIN) de 17 caracteres.
            string chasis = txtVChasis.Text.Trim();
            // [Actividad 5] Captura el texto del año de fabricación para su validación numérica.
            string anioTxt = txtVAnio.Text.Trim();
            // [Actividad 5] Captura y recorta el color exterior del vehículo.
            string color = txtVColor.Text.Trim();
            // [Actividad 5] Captura el texto del precio para su validación decimal.
            string precioTxt = txtVPrecio.Text.Trim();

            // [Actividad 5] Valida que los campos obligatorios principales (Marca, Modelo, Placa y Chasis) no estén vacíos.
            if (string.IsNullOrWhiteSpace(marca) || string.IsNullOrWhiteSpace(modelo) ||
                string.IsNullOrWhiteSpace(placa) || string.IsNullOrWhiteSpace(chasis))
            {
                // [Actividad 5] Muestra advertencia visual al operador cuando falta algún campo requerido.
                MessageBox.Show("Por favor complete los campos obligatorios: Marca, Modelo, Placa y Chasis.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                // [Actividad 5] Detiene el envío de la solicitud HTTP para evitar peticiones incompletas.
                return;
            }

            // [Actividad 5] Valida localmente la regla de negocio del chasis de exactamente 17 caracteres exigida por la API Java.
            if (chasis.Length != 17)
            {
                // [Actividad 5] Advierte al usuario que el chasis debe tener exactamente 17 caracteres.
                MessageBox.Show("El chasis debe contener exactamente 17 caracteres según el estándar de la API.",
                    "Validación de Chasis", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                // [Actividad 5] Devuelve el foco al campo de chasis para facilitar la corrección inmediata.
                txtVChasis.Focus();
                // [Actividad 5] Interrumpe el alta antes de invocar el endpoint POST /api/vehiculos.
                return;
            }

            // [Actividad 5] Valida que el año ingresado sea un entero válido dentro del rango aceptado (1900 a 2030).
            if (!int.TryParse(anioTxt, out int anio) || anio < 1900 || anio > 2030)
            {
                // [Actividad 5] Notifica que el año debe ser numérico de 4 dígitos dentro del rango válido.
                MessageBox.Show("Por favor ingrese un año válido de 4 dígitos (ej: 2024).",
                    "Validación de Año", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                // [Actividad 5] Enfoca el cuadro de texto del año para corregirlo.
                txtVAnio.Focus();
                // [Actividad 5] Cancela el flujo sin ejecutar la petición HTTP.
                return;
            }

            // [Actividad 5] Valida que el precio sea un número decimal positivo mayor a cero.
            if (!decimal.TryParse(precioTxt, out decimal precio) || precio <= 0)
            {
                // [Actividad 5] Advierte al operador que el precio debe ser un valor numérico positivo.
                MessageBox.Show("Por favor ingrese un precio numérico mayor a cero.",
                    "Validación de Precio", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                // [Actividad 5] Coloca el foco en el campo de precio para su corrección.
                txtVPrecio.Focus();
                // [Actividad 5] Cancela la operación antes de enviar la solicitud.
                return;
            }

            // [Actividad 5] Deshabilita el botón de alta durante la llamada asíncrona para evitar envíos duplicados.
            btnCrearVehiculo.Enabled = false;
            // [Actividad 5] Actualiza la etiqueta de estado informando que la solicitud viaja por NGINX (:8090).
            lblEstado.Text = "Estado: Registrando vehículo en Java a través de NGINX (:8090)...";
            // [Actividad 5] Colorea la etiqueta en azul indicando operación HTTP en progreso.
            lblEstado.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para controlar cualquier fallo inesperado durante el alta del vehículo.
            try
            {
                // [Actividad 1] [Actividad 5] Mapea los valores validados del formulario al contrato DTO CrearVehiculoRequest.
                var request = new CrearVehiculoRequest
                {
                    // [Actividad 1] Asigna la marca validada al DTO.
                    Marca = marca,
                    // [Actividad 1] Asigna el modelo validado al DTO.
                    Modelo = modelo,
                    // [Actividad 1] Asigna la placa única al DTO.
                    Placa = placa,
                    // [Actividad 1] Asigna el chasis de 17 caracteres al DTO.
                    Chasis = chasis,
                    // [Actividad 1] Asigna el año numérico al DTO.
                    Anio = anio,
                    // [Actividad 1] Asigna el color exterior al DTO.
                    Color = color,
                    // [Actividad 1] Asigna el precio decimal al DTO.
                    Precio = precio
                };

                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Invoca POST /api/vehiculos vía NGINX (:8090) con cabecera Bearer JWT.
                var response = await _vehiculoClient.CrearVehiculoAsync(token, request);

                // [Actividad 5] Evalúa si el servidor respondió exitosamente (HTTP 201 Created) con el VehiculoDto creado.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 5] [Actividad 10 - P27] Confirma visualmente el alta con el ID generado por Java Spring Boot.
                    lblEstado.Text = $"Estado: Vehículo registrado exitosamente con ID {response.Data.IdVehiculo} (HTTP 201 Created)";
                    // [Actividad 5] Colorea el estado en verde oscuro para indicar creación exitosa.
                    lblEstado.ForeColor = Color.DarkGreen;

                    // [Actividad 5] [Actividad 10 - P27] Muestra cuadro informativo con los datos confirmados del nuevo vehículo.
                    MessageBox.Show(
                        $"Vehículo registrado con éxito!\nID generado: {response.Data.IdVehiculo}\nMarca: {response.Data.Marca}\nModelo: {response.Data.Modelo}\nCódigo HTTP: 201 Created",
                        "Creación Exitosa", MessageBoxButtons.OK, MessageBoxIcon.Information);

                    // [Actividad 5] Limpia los campos únicos/variables del formulario de alta para preparar un nuevo registro.
                    // Limpiar formulario de alta
                    txtVPlaca.Clear();
                    // [Actividad 5] Limpia el campo de chasis para evitar reenvíos duplicados (HTTP 409).
                    txtVChasis.Clear();
                    // [Actividad 5] Limpia el campo de precio tras confirmar el alta.
                    txtVPrecio.Clear();

                    // [Actividad 5] [Actividad 10 - P27] Refresca automáticamente el listado del catálogo para reflejar el nuevo vehículo.
                    // Refrescar listado
                    btnConsultar_Click(sender, e);
                }
                else
                {
                    // [Actividad 5] [Actividad 11] Muestra en la barra de estado el mensaje del código HTTP (400, 401, 403, 409, 5xx).
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Resalta el error en color rojo carmesí.
                    lblEstado.ForeColor = Color.Crimson;

                    // [Actividad 5] [Actividad 11] Presenta el rechazo controlado al usuario sin simular éxito ni datos ficticios.
                    MessageBox.Show(response.ErrorMessage ?? "Error al registrar vehículo.",
                        "Error HTTP", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura cualquier excepción de UI o ejecución no prevista durante el evento de alta.
            catch (Exception ex)
            {
                // [Actividad 5] Muestra el mensaje de excepción en la barra de estado de la pestaña Vehículos.
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Colorea el estado en rojo carmesí para alertar del fallo.
                lblEstado.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Garantiza la reactivación del botón sin importar si la petición fue exitosa o falló.
            finally
            {
                // [Actividad 5] Rehabilita el botón "+ Crear Vehículo" al concluir la operación.
                btnCrearVehiculo.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] [Actividad 11] Operación 2: Listar catálogo en DataGridView y
        /// filtrar por marca (GET /api/vehiculos y GET /api/vehiculos?marca=... vía NGINX :8090),
        /// permitiendo también auditar qué vehículos pasaron a estado VENDIDO tras una venta en la PWA.
        /// </summary>
        private async void btnConsultar_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Recupera el token Bearer JWT del cuadro superior.
            string token = txtToken.Text.Trim();
            // [Actividad 5] Recupera el texto opcional de filtro por marca.
            string marca = txtFiltroMarca.Text.Trim();

            // [Actividad 5] Bloquea el botón de listado mientras se procesa la consulta HTTP.
            btnConsultar.Enabled = false;
            // [Actividad 5] Bloquea también la búsqueda por ID para evitar condiciones de carrera sobre el DataGridView.
            btnBuscarId.Enabled = false;
            // [Actividad 5] Informa en la barra de estado que se está consultando el catálogo vía NGINX (:8090).
            lblEstado.Text = "Estado: Consultando catálogo de vehículos en NGINX (:8090)...";
            // [Actividad 5] Cambia el texto de estado a azul durante la espera de respuesta.
            lblEstado.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para manejar errores durante la consulta del catálogo.
            try
            {
                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Invoca GET /api/vehiculos con filtro opcional por marca y Bearer JWT.
                var response = await _vehiculoClient.ListarVehiculosAsync(
                    token,
                    string.IsNullOrWhiteSpace(marca) ? null : marca
                );

                // [Actividad 5] Verifica si la respuesta HTTP fue exitosa y contiene la lista deserializada de VehiculoDto.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 1] [Actividad 5] [Actividad 10 - P27] Enlaza la lista real de VehiculoDto (incluyendo columna Estado) al DataGridView.
                    dgcVehiculos.DataSource = response.Data;
                    // [Actividad 5] Actualiza el mensaje de estado diferenciando catálogo completo vs. filtro por marca con su código HTTP.
                    lblEstado.Text = string.IsNullOrWhiteSpace(marca)
                        ? $"Estado: Catálogo completo cargado con éxito (HTTP {(int)response.StatusCode} OK)"
                        : $"Estado: Filtro por marca '{marca}' aplicado (HTTP {(int)response.StatusCode} OK)";
                    // [Actividad 5] Muestra el estado en verde oscuro indicando carga exitosa.
                    lblEstado.ForeColor = Color.DarkGreen;
                    // [Actividad 5] Actualiza el contador inferior con la cantidad exacta de vehículos devueltos.
                    lblTotal.Text = $"Total: {response.Data.Count} vehículos encontrados";
                }
                else
                {
                    // [Actividad 11] Vacía el DataGridView ante 401/403 u otro error para no dejar datos antiguos ni mostrar datos ficticios.
                    dgcVehiculos.DataSource = null;
                    // [Actividad 5] [Actividad 11] Refleja en pantalla el código y motivo del rechazo HTTP.
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Pinta el texto de estado en rojo carmesí.
                    lblEstado.ForeColor = Color.Crimson;
                    // [Actividad 11] Reinicia el contador a cero vehículos.
                    lblTotal.Text = "Total: 0 vehículos";

                    // [Actividad 5] [Actividad 11] Notifica el error HTTP (401, 403, 404, 5xx) mediante un diálogo de advertencia.
                    MessageBox.Show(
                        response.ErrorMessage ?? "Error al consultar el catálogo de vehículos.",
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura excepciones imprevistas durante el enlace de datos o invocación.
            catch (Exception ex)
            {
                // [Actividad 5] Reporta el fallo en la etiqueta de estado.
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Aplica color rojo carmesí para destacar el error.
                lblEstado.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Reactiva siempre ambos botones de consulta al finalizar.
            finally
            {
                // [Actividad 5] Habilita nuevamente el botón de listar/filtrar.
                btnConsultar.Enabled = true;
                // [Actividad 5] Habilita nuevamente el botón de búsqueda por ID.
                btnBuscarId.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] [Actividad 11] Consulta el detalle de un vehículo específico
        /// por su ID (GET /api/vehiculos/{id} vía NGINX :8090), permitiendo auditar su estado post-venta (VENDIDO).
        /// </summary>
        private async void btnBuscarId_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Lee el Bearer JWT ingresado en la cabecera del formulario.
            string token = txtToken.Text.Trim();
            // [Actividad 5] Lee el identificador de vehículo escrito por el operador.
            string idTexto = txtIdVehiculo.Text.Trim();

            // [Actividad 5] Valida que el ID sea un entero positivo mayor a cero antes de consumir la API.
            if (!int.TryParse(idTexto, out int id) || id <= 0)
            {
                // [Actividad 5] Informa al operador que debe ingresar un ID numérico válido.
                MessageBox.Show("Por favor ingrese un ID numérico válido mayor a 0.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Information);
                // [Actividad 5] Devuelve el foco al cuadro de texto del ID de vehículo.
                txtIdVehiculo.Focus();
                // [Actividad 5] Aborta la consulta para evitar peticiones malformadas.
                return;
            }

            // [Actividad 5] Deshabilita el botón de listado general mientras se consulta por ID.
            btnConsultar.Enabled = false;
            // [Actividad 5] Deshabilita el botón de búsqueda por ID para impedir clics repetidos.
            btnBuscarId.Enabled = false;
            // [Actividad 5] Indica visualmente que la búsqueda puntual está en curso sobre NGINX (:8090).
            lblEstado.Text = $"Estado: Buscando vehículo con ID {id} en NGINX (:8090)...";
            // [Actividad 5] Colorea el estado en azul mientras llega la respuesta.
            lblEstado.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para ejecutar la búsqueda puntual de forma segura.
            try
            {
                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Ejecuta GET /api/vehiculos/{id} con Bearer JWT vía NGINX (:8090).
                var response = await _vehiculoClient.ObtenerVehiculoPorIdAsync(token, id);

                // [Actividad 5] Evalúa si el vehículo existe y fue devuelto con HTTP 200 OK.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 1] [Actividad 10 - P27] Muestra el VehiculoDto individual dentro del DataGridView (incluyendo su Estado actual).
                    dgcVehiculos.DataSource = new List<VehiculoDto> { response.Data };
                    // [Actividad 5] Confirma hallazgo exitoso con HTTP 200 OK.
                    lblEstado.Text = $"Estado: Vehículo con ID {id} encontrado con éxito (HTTP 200 OK)";
                    // [Actividad 5] Colorea el mensaje de éxito en verde oscuro.
                    lblEstado.ForeColor = Color.DarkGreen;
                    // [Actividad 5] Actualiza el total mostrado a 1 vehículo.
                    lblTotal.Text = "Total: 1 vehículo";
                }
                else
                {
                    // [Actividad 11] Limpia el DataGridView ante 401/403/404 para nunca mostrar datos ficticios o desactualizados.
                    dgcVehiculos.DataSource = null;
                    // [Actividad 5] [Actividad 11] Expone el mensaje de error HTTP (401, 403, 404, 5xx) en la barra de estado.
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Colorea el mensaje en rojo carmesí.
                    lblEstado.ForeColor = Color.Crimson;
                    // [Actividad 11] Restablece el total a 0 vehículos.
                    lblTotal.Text = "Total: 0 vehículos";

                    // [Actividad 5] [Actividad 11] Presenta el diálogo de advertencia con el detalle del error HTTP.
                    MessageBox.Show(response.ErrorMessage ?? $"No se encontró el vehículo con ID {id}.",
                        "Detalle de Vehículo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura excepciones inesperadas en la capa de presentación.
            catch (Exception ex)
            {
                // [Actividad 5] Muestra la descripción del error en la barra de estado.
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Resalta el fallo en color rojo carmesí.
                lblEstado.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Restaura el estado habilitado de los botones de consulta al finalizar.
            finally
            {
                // [Actividad 5] Rehabilita el botón de consulta general.
                btnConsultar.Enabled = true;
                // [Actividad 5] Rehabilita el botón de búsqueda por ID.
                btnBuscarId.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] Limpia los filtros, resultados del DataGridView y etiquetas de estado de la pestaña Vehículos.
        /// </summary>
        private void btnLimpiarVehiculos_Click(object sender, EventArgs e)
        {
            // [Actividad 5] Borra el criterio de filtro por marca.
            txtFiltroMarca.Clear();
            // [Actividad 5] Borra el campo de búsqueda por ID de vehículo.
            txtIdVehiculo.Clear();
            // [Actividad 5] Desvincula cualquier origen de datos de la grilla de vehículos.
            dgcVehiculos.DataSource = null;
            // [Actividad 5] Restablece la etiqueta de estado al texto inicial en espera.
            lblEstado.Text = "Estado: En espera de consulta";
            // [Actividad 5] Restaura el color gris neutro de la etiqueta de estado.
            lblEstado.ForeColor = Color.DimGray;
            // [Actividad 5] Reinicia el contador inferior a cero vehículos.
            lblTotal.Text = "Total: 0 vehículos";
        }

        // ==========================================
        // 2. COMPRADORES (Python FastAPI vía NGINX :8090)
        // [Actividad 5] [Actividad 10 - P27] [Actividad 11]
        // ==========================================

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] [Actividad 11] Operación 3: Registrar comprador
        /// (POST /api/compradores vía NGINX :8090), dejándolo disponible para el flujo transversal de ventas.
        /// </summary>
        private async void btnCrearComprador_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Obtiene el Bearer JWT configurado en la cabecera de la ventana.
            string token = txtToken.Text.Trim();
            // [Actividad 5] Obtiene y recorta el documento de identidad del comprador.
            string doc = txtCDocumento.Text.Trim();
            // [Actividad 5] Obtiene y recorta el nombre completo del comprador.
            string nom = txtCNombre.Text.Trim();
            // [Actividad 5] Obtiene y recorta el correo electrónico del comprador.
            string cor = txtCCorreo.Text.Trim();

            // [Actividad 5] Verifica que Documento, Nombre y Correo estén completos antes de llamar a FastAPI.
            if (string.IsNullOrWhiteSpace(doc) || string.IsNullOrWhiteSpace(nom) || string.IsNullOrWhiteSpace(cor))
            {
                // [Actividad 5] Advierte al usuario que los tres campos son obligatorios para crear un comprador.
                MessageBox.Show("Por favor ingrese todos los datos: Documento, Nombre y Correo electrónico.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                // [Actividad 5] Cancela el envío para evitar errores 400/422 evitables.
                return;
            }

            // [Actividad 5] Deshabilita el botón de registro durante la llamada asíncrona para prevenir duplicados.
            btnCrearComprador.Enabled = false;
            // [Actividad 5] Informa en la pestaña Compradores que la petición se envía a FastAPI vía NGINX (:8090).
            lblEstadoCompradores.Text = "Estado: Registrando comprador en FastAPI (:8090)...";
            // [Actividad 5] Cambia el color de estado a azul mientras dura la solicitud.
            lblEstadoCompradores.ForeColor = Color.Blue;

            // [Actividad 5] Protege el proceso de alta ante cualquier error inesperado.
            try
            {
                // [Actividad 1] [Actividad 5] Construye el contrato DTO CrearCompradorRequest con los campos validados.
                var request = new CrearCompradorRequest
                {
                    // [Actividad 1] Asigna el documento del comprador al DTO.
                    Documento = doc,
                    // [Actividad 1] Asigna el nombre completo del comprador al DTO.
                    Nombre = nom,
                    // [Actividad 1] Asigna el correo electrónico del comprador al DTO.
                    Correo = cor
                };

                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Invoca POST /api/compradores con Bearer JWT vía NGINX (:8090).
                var response = await _ventaClient.CrearCompradorAsync(token, request);

                // [Actividad 5] Verifica si FastAPI creó correctamente el comprador (HTTP 201 Created) y devolvió CompradorDto.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 5] Muestra el ID asignado al nuevo comprador en la etiqueta de estado.
                    lblEstadoCompradores.Text = $"Estado: Comprador registrado exitosamente con ID {response.Data.Id} (HTTP 201 Created)";
                    // [Actividad 5] Resalta la confirmación en color verde oscuro.
                    lblEstadoCompradores.ForeColor = Color.DarkGreen;

                    // [Actividad 5] Muestra un cuadro informativo con los datos confirmados del registro creado.
                    MessageBox.Show(
                        $"Comprador registrado con éxito!\nID generado: {response.Data.Id}\nNombre: {response.Data.Nombre}\nDocumento: {response.Data.Documento}\nCódigo HTTP: 201 Created",
                        "Registro Exitoso", MessageBoxButtons.OK, MessageBoxIcon.Information);

                    // [Actividad 5] Limpia el campo Documento tras completar el alta.
                    txtCDocumento.Clear();
                    // [Actividad 5] Limpia el campo Nombre tras completar el alta.
                    txtCNombre.Clear();
                    // [Actividad 5] Limpia el campo Correo tras completar el alta.
                    txtCCorreo.Clear();

                    // [Actividad 5] Refresca inmediatamente el catálogo de compradores para reflejar el nuevo registro.
                    btnListarCompradores_Click(sender, e);
                }
                else
                {
                    // [Actividad 5] [Actividad 11] Muestra en la barra de estado el rechazo HTTP (400, 401, 403, 409, 5xx).
                    lblEstadoCompradores.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Colorea el texto de error en rojo carmesí.
                    lblEstadoCompradores.ForeColor = Color.Crimson;

                    // [Actividad 5] [Actividad 11] Presenta el diálogo de advertencia sin desactivar seguridad ni fingir éxito.
                    MessageBox.Show(response.ErrorMessage ?? "Error al registrar comprador.",
                        "Error HTTP", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura excepciones no previstas durante el registro del comprador.
            catch (Exception ex)
            {
                // [Actividad 5] Expone el mensaje de error en la barra de estado de Compradores.
                lblEstadoCompradores.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Pinta el mensaje en rojo carmesí.
                lblEstadoCompradores.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Restaura el botón "+ Crear Comprador" al concluir el bloque try/catch.
            finally
            {
                // [Actividad 5] Rehabilita el botón de registro de compradores.
                btnCrearComprador.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] [Actividad 11] Operación 4: Listar compradores autorizados (GET /api/compradores vía NGINX :8090).
        /// </summary>
        private async void btnListarCompradores_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Obtiene el token Bearer JWT desde el cuadro de texto superior.
            string token = txtToken.Text.Trim();

            // [Actividad 5] Deshabilita el botón durante la consulta HTTP para evitar peticiones solapadas.
            btnListarCompradores.Enabled = false;
            // [Actividad 5] Indica en la barra de estado que se está consultando GET /api/compradores en NGINX (:8090).
            lblEstadoCompradores.Text = "Estado: Consultando listado de compradores en NGINX (:8090)...";
            // [Actividad 5] Colorea la etiqueta en azul durante la espera de respuesta.
            lblEstadoCompradores.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para el consumo y enlace de datos de compradores.
            try
            {
                // [Actividad 5] [Actividad 11] Invoca GET /api/compradores enviando el Bearer JWT hacia NGINX (:8090).
                var response = await _ventaClient.ListarCompradoresAsync(token);

                // [Actividad 5] Evalúa si la respuesta fue HTTP 200 OK y trae la colección de CompradorDto.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 1] [Actividad 5] Enlaza la lista real de CompradorDto al DataGridView de compradores.
                    dgcCompradores.DataSource = response.Data;
                    // [Actividad 5] Actualiza el estado con la cantidad de compradores cargados y HTTP 200 OK.
                    lblEstadoCompradores.Text = $"Estado: {response.Data.Count} compradores cargados con éxito (HTTP 200 OK)";
                    // [Actividad 5] Colorea la confirmación en verde oscuro.
                    lblEstadoCompradores.ForeColor = Color.DarkGreen;
                    // [Actividad 5] Actualiza el contador inferior de compradores.
                    lblTotalCompradores.Text = $"Total: {response.Data.Count} compradores";
                }
                else
                {
                    // [Actividad 11] Vacía el DataGridView ante rechazos 401/403 o errores HTTP para nunca mostrar datos ficticios.
                    dgcCompradores.DataSource = null;
                    // [Actividad 5] [Actividad 11] Muestra el diagnóstico del error HTTP en la barra de estado.
                    lblEstadoCompradores.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Colorea el estado de error en rojo carmesí.
                    lblEstadoCompradores.ForeColor = Color.Crimson;
                    // [Actividad 11] Restablece el contador de compradores a cero.
                    lblTotalCompradores.Text = "Total: 0 compradores";

                    // [Actividad 5] [Actividad 11] Informa visualmente el error HTTP mediante MessageBox.
                    MessageBox.Show(response.ErrorMessage ?? "Error al listar compradores.",
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura cualquier fallo imprevisto durante la actualización visual.
            catch (Exception ex)
            {
                // [Actividad 5] Informa la excepción en la etiqueta de estado de Compradores.
                lblEstadoCompradores.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Colorea el mensaje en rojo carmesí.
                lblEstadoCompradores.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Garantiza la reactivación del botón tras concluir la consulta.
            finally
            {
                // [Actividad 5] Rehabilita el botón "Listar Compradores".
                btnListarCompradores.Enabled = true;
            }
        }

        // ==========================================
        // 3. VENTAS (Python FastAPI vía NGINX :8090 - Consulta Exclusiva)
        // [Actividad 5]: SOLO GET /api/ventas y GET /api/ventas/{id}. PROHIBIDO POST /api/ventas.
        // [Actividad 10 - P27]: Auditoría transversal post-venta.
        // [Actividad 11]: Rechazo controlado 401/403 sin datos ficticios.
        // ==========================================

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] [Actividad 11] Operación 5A: Listar histórico general de ventas
        /// en modo SOLO CONSULTA (GET /api/ventas vía NGINX :8090). Prohibido registrar ventas desde aquí.
        /// </summary>
        private async void btnListarVentas_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Obtiene el token Bearer JWT configurado en la cabecera.
            string token = txtToken.Text.Trim();

            // [Actividad 5] Deshabilita el botón mientras se consulta el histórico de ventas.
            btnListarVentas.Enabled = false;
            // [Actividad 5] Informa en la barra de estado que la consulta viaja a FastAPI vía NGINX (:8090).
            lblEstadoVenta.Text = "Estado: Consultando listado de ventas en FastAPI (:8090)...";
            // [Actividad 5] Colorea el estado en azul mientras se recibe la respuesta.
            lblEstadoVenta.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para el consumo y visualización del histórico de ventas.
            try
            {
                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Invoca GET /api/ventas (solo consulta) con Bearer JWT vía NGINX (:8090).
                var response = await _ventaClient.ListarVentasAsync(token);

                // [Actividad 5] Comprueba si la API retornó HTTP 200 OK junto con la lista de VentaDto.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 1] [Actividad 5] [Actividad 10 - P27] Carga las ventas reales en el DataGridView para seguimiento y auditoría.
                    dgcVentas.DataSource = response.Data;
                    // [Actividad 5] Muestra el resumen de ventas recuperadas con código HTTP 200 OK.
                    lblEstadoVenta.Text = $"Estado: {response.Data.Count} ventas consultadas con éxito (HTTP 200 OK)";
                    // [Actividad 5] Resalta el estado de éxito en verde oscuro.
                    lblEstadoVenta.ForeColor = Color.DarkGreen;
                }
                else
                {
                    // [Actividad 11] Limpia la grilla de ventas ante 401/403 u otros errores para evitar datos ficticios o obsoletos.
                    dgcVentas.DataSource = null;
                    // [Actividad 5] [Actividad 11] Muestra el detalle del código HTTP rechazado en la etiqueta de estado.
                    lblEstadoVenta.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Colorea el error en rojo carmesí.
                    lblEstadoVenta.ForeColor = Color.Crimson;

                    // [Actividad 5] [Actividad 11] Muestra el cuadro de advertencia con el error HTTP exacto.
                    MessageBox.Show(response.ErrorMessage ?? "Error al consultar ventas.",
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura excepciones no controladas durante la consulta de ventas.
            catch (Exception ex)
            {
                // [Actividad 5] Muestra el mensaje de la excepción en la barra de estado de Ventas.
                lblEstadoVenta.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Aplica color rojo carmesí para indicar error.
                lblEstadoVenta.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Rehabilita el botón al concluir la llamada HTTP.
            finally
            {
                // [Actividad 5] Restaura el botón "Listar Todas las Ventas".
                btnListarVentas.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] [Actividad 11] Operación 5B: Consultar una venta individual por su ID
        /// en modo SOLO CONSULTA (GET /api/ventas/{id} vía NGINX :8090) para auditar vehículo, comprador,
        /// estado de la transacción y estado de notificación.
        /// </summary>
        private async void btnConsultarVenta_Click(object sender, EventArgs e)
        {
            // [Actividad 5] [Actividad 11] Recupera el token Bearer JWT de la barra superior.
            string token = txtToken.Text.Trim();
            // [Actividad 5] Recupera el texto del ID de venta a consultar.
            string idTexto = txtIdVenta.Text.Trim();

            // [Actividad 5] Valida que el ID de venta ingresado sea un entero positivo mayor a cero.
            if (!int.TryParse(idTexto, out int idVenta) || idVenta <= 0)
            {
                // [Actividad 5] Solicita al usuario un identificador numérico válido de venta.
                MessageBox.Show("Por favor ingrese un ID de venta numérico válido mayor a 0.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Information);
                // [Actividad 5] Coloca el foco en el cuadro txtIdVenta para facilitar la corrección.
                txtIdVenta.Focus();
                // [Actividad 5] Cancela la consulta sin invocar la API.
                return;
            }

            // [Actividad 5] Deshabilita el botón de búsqueda individual durante la solicitud asíncrona.
            btnConsultarVenta.Enabled = false;
            // [Actividad 5] Muestra en la barra de estado que se está consultando GET /api/ventas/{id} en NGINX (:8090).
            lblEstadoVenta.Text = $"Estado: Consultando venta con ID {idVenta} en NGINX (:8090)...";
            // [Actividad 5] Colorea el estado en azul mientras se espera respuesta.
            lblEstadoVenta.ForeColor = Color.Blue;

            // [Actividad 5] Bloque protegido para ejecutar la consulta puntual de venta.
            try
            {
                // [Actividad 5] [Actividad 10 - P27] [Actividad 11] Invoca GET /api/ventas/{idVenta} (solo lectura) con Bearer JWT vía NGINX (:8090).
                var response = await _ventaClient.ObtenerVentaPorIdAsync(token, idVenta);

                // [Actividad 5] Evalúa si la venta fue localizada con HTTP 200 OK y mapeada a VentaDto.
                if (response.IsSuccess && response.Data != null)
                {
                    // [Actividad 1] [Actividad 5] [Actividad 10 - P27] Renderiza todos los campos auditables de VentaDto en el panel de detalle lateral.
                    MostrarDetalleVenta(response.Data);
                    // [Actividad 5] Confirma visualmente el éxito de la consulta individual.
                    lblEstadoVenta.Text = "Estado: Venta consultada con éxito (HTTP 200 OK)";
                    // [Actividad 5] Colorea la etiqueta de estado en verde oscuro.
                    lblEstadoVenta.ForeColor = Color.DarkGreen;
                }
                else
                {
                    // [Actividad 11] Limpia el panel de detalle ante 401/403/404 para no dejar ni simular información de ventas.
                    LimpiarDetalleVenta();
                    // [Actividad 5] [Actividad 11] Muestra el error HTTP específico en la etiqueta de estado.
                    lblEstadoVenta.Text = $"Estado: {response.ErrorMessage}";
                    // [Actividad 5] [Actividad 11] Resalta el error en rojo carmesí.
                    lblEstadoVenta.ForeColor = Color.Crimson;

                    // [Actividad 5] [Actividad 11] Presenta el diálogo con el detalle del rechazo o ausencia (401/403/404/5xx).
                    MessageBox.Show(response.ErrorMessage ?? "Error al consultar la venta.",
                        "Consulta de Venta", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            // [Actividad 5] Captura excepciones inesperadas en la capa de presentación.
            catch (Exception ex)
            {
                // [Actividad 5] Reporta el mensaje de excepción en la etiqueta de estado de Ventas.
                lblEstadoVenta.Text = $"Estado: Error - {ex.Message}";
                // [Actividad 5] Resalta la excepción en rojo carmesí.
                lblEstadoVenta.ForeColor = Color.Crimson;
            }
            // [Actividad 5] Reactiva siempre el botón "Buscar por ID" al concluir el intento.
            finally
            {
                // [Actividad 5] Habilita nuevamente el botón de consulta individual de venta.
                btnConsultarVenta.Enabled = true;
            }
        }

        /// <summary>
        /// [Actividad 5] [Actividad 10 - P27] Sincroniza automáticamente el panel lateral de detalle
        /// cuando el usuario selecciona una fila en el listado histórico dgcVentas.
        /// </summary>
        private void dgcVentas_SelectionChanged(object sender, EventArgs e)
        {
            // [Actividad 1] [Actividad 5] Verifica mediante pattern matching que la fila activa esté vinculada a un objeto VentaDto válido.
            if (dgcVentas.CurrentRow?.DataBoundItem is VentaDto seleccionada)
            {
                // [Actividad 5] [Actividad 10 - P27] Muestra los metadatos completos de la venta seleccionada en el panel de auditoría.
                MostrarDetalleVenta(seleccionada);
            }
        }

        /// <summary>
        /// [Actividad 1] [Actividad 5] [Actividad 10 - P27] Proyecta las propiedades del contrato VentaDto
        /// sobre las etiquetas del panel de detalle individual, resaltando con colores semánticos el
        /// estado de la venta y el estado de la notificación post-venta.
        /// </summary>
        private void MostrarDetalleVenta(VentaDto v)
        {
            // [Actividad 1] Muestra el identificador numérico de la venta.
            lblValorVentaId.Text = v.Id.ToString();
            // [Actividad 1] [Actividad 10 - P27] Muestra el UUID de operación o guion "-" si viene nulo.
            lblValorOperacionId.Text = v.OperacionId ?? "-";
            // [Actividad 1] [Actividad 10 - P27] Muestra el ID del vehículo involucrado para cruzar auditoría de estado VENDIDO.
            lblValorVehiculoId.Text = v.VehiculoId.ToString();
            // [Actividad 1] [Actividad 10 - P27] Muestra el ID del comprador asociado a la venta.
            lblValorCompradorId.Text = v.CompradorId.ToString();
            // [Actividad 1] Muestra el precio acordado o "-" si no está definido.
            lblValorPrecio.Text = v.PrecioAcordado ?? "-";
            // [Actividad 1] Muestra el código de moneda (ej. USD) o "-" si es nulo.
            lblValorMoneda.Text = v.Moneda ?? "-";
            // [Actividad 1] Muestra la fecha y hora UTC de registro de la venta o "-" si es nula.
            lblValorFecha.Text = v.FechaUTC ?? "-";

            // [Actividad 10 - P27] Muestra el estado actual de la transacción de venta.
            lblValorEstadoVenta.Text = v.Estado ?? "-";
            // [Actividad 5] [Actividad 10 - P27] Aplica código de colores según el estado de orquestación de la venta.
            lblValorEstadoVenta.ForeColor = (v.Estado?.ToUpperInvariant()) switch
            {
                // [Actividad 10 - P27] Verde oscuro cuando la venta se completó satisfactoriamente (vehículo pasado a VENDIDO).
                "COMPLETADA" => Color.DarkGreen,
                // [Actividad 10 - P27] Naranja oscuro cuando la venta requiere revisión o sigue en proceso.
                "EN_REVISION" => Color.DarkOrange,
                // [Actividad 10 - P27] Rojo cuando la transacción falló o fue compensada.
                "FALLIDA" => Color.Red,
                // [Actividad 5] Negro por defecto para cualquier otro estado no catalogado.
                _ => Color.Black
            };

            // [Actividad 10 - P27] Muestra el estado de la notificación emitida por la venta.
            lblValorNotificacion.Text = v.Notificacion ?? "-";
            // [Actividad 5] [Actividad 10 - P27] Colorea el estado de la notificación para facilitar la auditoría visual.
            lblValorNotificacion.ForeColor = (v.Notificacion?.ToUpperInvariant()) switch
            {
                // [Actividad 10 - P27] Verde oscuro cuando la notificación fue enviada correctamente.
                "ENVIADA" => Color.DarkGreen,
                // [Actividad 10 - P27] Naranja oscuro cuando la notificación aún se encuentra pendiente.
                "PENDIENTE" => Color.DarkOrange,
                // [Actividad 5] Gris pizarra oscuro como color neutro predeterminado.
                _ => Color.DarkSlateGray
            };
        }

        /// <summary>
        /// [Actividad 5] [Actividad 11] Restablece todos los campos del panel de detalle de venta a "-"
        /// ante consultas fallidas o rechazos 401/403/404, garantizando que nunca se muestren datos ficticios o residuales.
        /// </summary>
        private void LimpiarDetalleVenta()
        {
            // [Actividad 11] Limpia el ID de venta mostrado.
            lblValorVentaId.Text = "-";
            // [Actividad 11] Limpia el UUID de operación mostrado.
            lblValorOperacionId.Text = "-";
            // [Actividad 11] Limpia el ID de vehículo mostrado.
            lblValorVehiculoId.Text = "-";
            // [Actividad 11] Limpia el ID de comprador mostrado.
            lblValorCompradorId.Text = "-";
            // [Actividad 11] Limpia el precio acordado mostrado.
            lblValorPrecio.Text = "-";
            // [Actividad 11] Limpia la moneda mostrada.
            lblValorMoneda.Text = "-";
            // [Actividad 11] Limpia la fecha UTC mostrada.
            lblValorFecha.Text = "-";
            // [Actividad 11] Limpia el estado de la venta mostrado.
            lblValorEstadoVenta.Text = "-";
            // [Actividad 5] Restaura el color neutro negro para la etiqueta de estado de venta.
            lblValorEstadoVenta.ForeColor = Color.Black;
            // [Actividad 11] Limpia el estado de notificación mostrado.
            lblValorNotificacion.Text = "-";
            // [Actividad 5] Restaura el color neutro gris pizarra para la etiqueta de notificación.
            lblValorNotificacion.ForeColor = Color.DarkSlateGray;
        }
    }
}
