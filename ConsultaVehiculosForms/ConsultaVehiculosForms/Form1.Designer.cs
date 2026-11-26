// =================================================================================================
// Archivo: Form1.Designer.cs
// Proyecto: ConsultaVehiculosForms (Cliente Administrativo de Escritorio - Windows Forms)
// Propósito: Archivo parcial generado por el diseñador de Windows Forms que construye, posiciona,
//            enlaza eventos y libera los controles visuales del formulario principal Form1:
//              - Cabecera global de arquitectura (Gateway NGINX :8090) y campo de Token Bearer JWT.
//              - Pestaña 1 (tabVehiculos): alta (POST /api/vehiculos), listado/filtro por marca y
//                búsqueda por ID (GET /api/vehiculos).
//              - Pestaña 2 (tabCompradores): alta (POST /api/compradores) y listado (GET /api/compradores).
//              - Pestaña 3 (tabVentas): consulta exclusiva de ventas (GET /api/ventas y GET /api/ventas/{id})
//                con aviso explícito de que POST /api/ventas es exclusivo de la PWA móvil.
// Actividades APE 04 que implementa:
//   - [Actividad 5]: Diseño completo de la interfaz administrativa Windows Forms (.NET 8 C#) con
//                    entrada de Bearer JWT, 3 pestañas diferenciadas, prohibición visual y funcional
//                    de POST /api/ventas y etiquetas de estado para retroalimentación de códigos HTTP.
//   - [Actividad 10 - P27]: Controles de alta administrativa, grillas de verificación y panel de
//                           detalle para auditoría post-venta (vehículo VENDIDO y estado de venta).
//   - [Actividad 11]: Campo de token JWT (sin valores incrustados) y etiquetas de estado/grillas
//                     preparadas para reflejar rechazos controlados 401/403 sin datos ficticios.
// =================================================================================================

// [Actividad 5] Espacio de nombres raíz del proyecto administrativo Windows Forms.
namespace ConsultaVehiculosForms
{
    // [Actividad 5] Declaración parcial de Form1 reservada para la definición visual y gestión de componentes del diseñador.
    partial class Form1
    {
        // [Actividad 5] Contenedor de componentes administrados requerido por el diseñador de Windows Forms para limpieza en Dispose.
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// [Actividad 5] Libera los recursos administrados utilizados por el formulario y sus controles al cerrarse la ventana.
        /// </summary>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// [Actividad 5] Método generado por el diseñador que instancia, configura y ensambla toda la
        /// jerarquía visual de Form1 (cabecera JWT y las tres pestañas administrativas).
        /// </summary>
        private void InitializeComponent()
        {
            // [Actividad 5] [Actividad 11] Instanciación de controles de cabecera: título, subtítulo NGINX (:8090), entrada de Token Bearer JWT y contenedor de pestañas.
            lblTitulo = new Label();
            lblSubtitulo = new Label();
            lblToken = new Label();
            txtToken = new TextBox();
            tabControlPrincipal = new TabControl();
            
            // [Actividad 5] [Actividad 10 - P27] Tab 1: Vehículos - Instanciación de la pestaña y grupo de alta (POST /api/vehiculos).
            // Tab 1: Vehiculos
            tabVehiculos = new TabPage();
            grpCrearVehiculo = new GroupBox();
            lblVMarga = new Label();
            txtVMarca = new TextBox();
            lblVModelo = new Label();
            txtVModelo = new TextBox();
            lblVPlaca = new Label();
            txtVPlaca = new TextBox();
            lblVChasis = new Label();
            txtVChasis = new TextBox();
            lblVAnio = new Label();
            txtVAnio = new TextBox();
            lblVColor = new Label();
            txtVColor = new TextBox();
            lblVPrecio = new Label();
            txtVPrecio = new TextBox();
            btnCrearVehiculo = new Button();
            
            // [Actividad 5] [Actividad 10 - P27] Tab 1: Vehículos - Instanciación del grupo de consulta, filtro por marca, búsqueda por ID y DataGridView del catálogo.
            grpListarVehiculos = new GroupBox();
            lblFiltroMarca = new Label();
            txtFiltroMarca = new TextBox();
            btnConsultar = new Button();
            lblIdVehiculo = new Label();
            txtIdVehiculo = new TextBox();
            btnBuscarId = new Button();
            btnLimpiarVehiculos = new Button();
            lblEstado = new Label();
            dgcVehiculos = new DataGridView();
            lblTotal = new Label();

            // [Actividad 5] [Actividad 10 - P27] Tab 2: Compradores - Instanciación de la pestaña y grupo de registro (POST /api/compradores).
            // Tab 2: Compradores
            tabCompradores = new TabPage();
            grpCrearComprador = new GroupBox();
            lblCDocumento = new Label();
            txtCDocumento = new TextBox();
            lblCNombre = new Label();
            txtCNombre = new TextBox();
            lblCCorreo = new Label();
            txtCCorreo = new TextBox();
            btnCrearComprador = new Button();

            // [Actividad 5] Tab 2: Compradores - Instanciación del grupo de consulta y DataGridView de compradores (GET /api/compradores).
            grpListarCompradores = new GroupBox();
            btnListarCompradores = new Button();
            lblEstadoCompradores = new Label();
            dgcCompradores = new DataGridView();
            lblTotalCompradores = new Label();

            // [Actividad 5] [Actividad 10 - P27] Tab 3: Ventas (SOLO CONSULTA) - Instanciación de aviso arquitectónico, controles GET /api/ventas y GET /api/ventas/{id}, grilla y panel de auditoría individual.
            // Tab 3: Ventas
            tabVentas = new TabPage();
            lblAvisoVentas = new Label();
            lblIdVenta = new Label();
            txtIdVenta = new TextBox();
            btnConsultarVenta = new Button();
            btnListarVentas = new Button();
            lblEstadoVenta = new Label();
            dgcVentas = new DataGridView();
            grpDetalleVenta = new GroupBox();
            lblValorFecha = new Label();
            lblEtiqFecha = new Label();
            lblValorNotificacion = new Label();
            lblEtiqNotif = new Label();
            lblValorEstadoVenta = new Label();
            lblEtiqEstadoVenta = new Label();
            lblValorMoneda = new Label();
            lblEtiqMoneda = new Label();
            lblValorPrecio = new Label();
            lblEtiqPrecio = new Label();
            lblValorCompradorId = new Label();
            lblEtiqCompradorId = new Label();
            lblValorVehiculoId = new Label();
            lblEtiqVehiculoId = new Label();
            lblValorOperacionId = new Label();
            lblEtiqOperacionId = new Label();
            lblValorVentaId = new Label();
            lblEtiqVentaId = new Label();

            // [Actividad 5] Suspensión temporal del motor de layout e inicio de carga en lote de los DataGridView para optimizar el renderizado inicial.
            tabControlPrincipal.SuspendLayout();
            tabVehiculos.SuspendLayout();
            grpCrearVehiculo.SuspendLayout();
            grpListarVehiculos.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)dgcVehiculos).BeginInit();

            tabCompradores.SuspendLayout();
            grpCrearComprador.SuspendLayout();
            grpListarCompradores.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)dgcCompradores).BeginInit();

            tabVentas.SuspendLayout();
            ((System.ComponentModel.ISupportInitialize)dgcVentas).BeginInit();
            grpDetalleVenta.SuspendLayout();
            SuspendLayout();

            // [Actividad 5] Bloque de cabecera: etiqueta principal del cliente administrativo Windows Forms.
            // lblTitulo
            lblTitulo.AutoSize = true;
            lblTitulo.Font = new Font("Segoe UI", 13F, FontStyle.Bold);
            lblTitulo.Location = new Point(25, 12);
            lblTitulo.Name = "lblTitulo";
            lblTitulo.Size = new Size(540, 25);
            lblTitulo.TabIndex = 0;
            lblTitulo.Text = "SOA APE 04 - Administración Multicanal (Windows Forms)";

            // [Actividad 5] Subtítulo informativo que evidencia el consumo unificado vía Gateway NGINX (:8090) hacia Spring Boot y FastAPI.
            // lblSubtitulo
            lblSubtitulo.AutoSize = true;
            lblSubtitulo.ForeColor = Color.DarkSlateGray;
            lblSubtitulo.Location = new Point(26, 38);
            lblSubtitulo.Name = "lblSubtitulo";
            lblSubtitulo.Size = new Size(620, 15);
            lblSubtitulo.TabIndex = 1;
            lblSubtitulo.Text = "Conectado al Gateway NGINX (:8090) hacia Spring Boot (:8080/:8082) y Python FastAPI (:8002)";

            // [Actividad 5] [Actividad 11] Etiqueta descriptiva para el campo compartido de autenticación Bearer JWT.
            // lblToken
            lblToken.AutoSize = true;
            lblToken.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblToken.Location = new Point(26, 62);
            lblToken.Name = "lblToken";
            lblToken.Size = new Size(69, 15);
            lblToken.TabIndex = 2;
            lblToken.Text = "Token JWT:";

            // [Actividad 5] [Actividad 11] Cuadro de texto compartido para pegar el Bearer JWT emitido por /oauth/token; se envía en todas las peticiones protegidas.
            // txtToken
            txtToken.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtToken.Location = new Point(101, 59);
            txtToken.Name = "txtToken";
            txtToken.PlaceholderText = "Pegue aquí el Bearer Token JWT (obtenido en /oauth/token)";
            txtToken.Size = new Size(865, 23);
            txtToken.TabIndex = 3;

            // [Actividad 5] Contenedor principal de pestañas que organiza los 3 módulos administrativos: Vehículos, Compradores y Ventas (solo consulta).
            // tabControlPrincipal
            tabControlPrincipal.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            tabControlPrincipal.Controls.Add(tabVehiculos);
            tabControlPrincipal.Controls.Add(tabCompradores);
            tabControlPrincipal.Controls.Add(tabVentas);
            tabControlPrincipal.Location = new Point(25, 90);
            tabControlPrincipal.Name = "tabControlPrincipal";
            tabControlPrincipal.SelectedIndex = 0;
            tabControlPrincipal.Size = new Size(945, 540);
            tabControlPrincipal.TabIndex = 4;

            // ==========================================
            // [Actividad 5] [Actividad 10 - P27] TAB 1: VEHÍCULOS (POST / GET /api/vehiculos y filtro por marca)
            // ==========================================
            tabVehiculos.Controls.Add(grpCrearVehiculo);
            tabVehiculos.Controls.Add(grpListarVehiculos);
            tabVehiculos.Location = new Point(4, 24);
            tabVehiculos.Name = "tabVehiculos";
            tabVehiculos.Padding = new Padding(8);
            tabVehiculos.Size = new Size(937, 512);
            tabVehiculos.TabIndex = 0;
            tabVehiculos.Text = "1. Vehículos (POST / GET)";
            tabVehiculos.UseVisualStyleBackColor = true;

            // [Actividad 5] [Actividad 10 - P27] Contenedor visual del formulario de alta de vehículo (POST /api/vehiculos - requiere OPERADOR/ADMIN).
            // grpCrearVehiculo
            grpCrearVehiculo.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            grpCrearVehiculo.Controls.Add(lblVMarga);
            grpCrearVehiculo.Controls.Add(txtVMarca);
            grpCrearVehiculo.Controls.Add(lblVModelo);
            grpCrearVehiculo.Controls.Add(txtVModelo);
            grpCrearVehiculo.Controls.Add(lblVPlaca);
            grpCrearVehiculo.Controls.Add(txtVPlaca);
            grpCrearVehiculo.Controls.Add(lblVChasis);
            grpCrearVehiculo.Controls.Add(txtVChasis);
            grpCrearVehiculo.Controls.Add(lblVAnio);
            grpCrearVehiculo.Controls.Add(txtVAnio);
            grpCrearVehiculo.Controls.Add(lblVColor);
            grpCrearVehiculo.Controls.Add(txtVColor);
            grpCrearVehiculo.Controls.Add(lblVPrecio);
            grpCrearVehiculo.Controls.Add(txtVPrecio);
            grpCrearVehiculo.Controls.Add(btnCrearVehiculo);
            grpCrearVehiculo.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            grpCrearVehiculo.Location = new Point(10, 8);
            grpCrearVehiculo.Name = "grpCrearVehiculo";
            grpCrearVehiculo.Size = new Size(915, 95);
            grpCrearVehiculo.TabIndex = 0;
            grpCrearVehiculo.TabStop = false;
            grpCrearVehiculo.Text = "Dar de Alta Vehículo (POST /api/vehiculos - Requiere OPERADOR o ADMIN)";

            // [Actividad 5] Campos de captura del contrato CrearVehiculoRequest: Marca, Modelo, Placa, Chasis (17 car.), Año, Color y Precio.
            lblVMarga.Font = new Font("Segoe UI", 8.5F);
            lblVMarga.Location = new Point(10, 25);
            lblVMarga.Name = "lblVMarga";
            lblVMarga.Size = new Size(45, 15);
            lblVMarga.Text = "Marca:";

            txtVMarca.Font = new Font("Segoe UI", 8.5F);
            txtVMarca.Location = new Point(55, 22);
            txtVMarca.Name = "txtVMarca";
            txtVMarca.PlaceholderText = "Toyota";
            txtVMarca.Size = new Size(95, 23);

            lblVModelo.Font = new Font("Segoe UI", 8.5F);
            lblVModelo.Location = new Point(155, 25);
            lblVModelo.Name = "lblVModelo";
            lblVModelo.Size = new Size(50, 15);
            lblVModelo.Text = "Modelo:";

            txtVModelo.Font = new Font("Segoe UI", 8.5F);
            txtVModelo.Location = new Point(208, 22);
            txtVModelo.Name = "txtVModelo";
            txtVModelo.PlaceholderText = "Yaris";
            txtVModelo.Size = new Size(95, 23);

            lblVPlaca.Font = new Font("Segoe UI", 8.5F);
            lblVPlaca.Location = new Point(310, 25);
            lblVPlaca.Name = "lblVPlaca";
            lblVPlaca.Size = new Size(40, 15);
            lblVPlaca.Text = "Placa:";

            txtVPlaca.Font = new Font("Segoe UI", 8.5F);
            txtVPlaca.Location = new Point(350, 22);
            txtVPlaca.Name = "txtVPlaca";
            txtVPlaca.PlaceholderText = "PBA1234";
            txtVPlaca.Size = new Size(80, 23);

            lblVChasis.Font = new Font("Segoe UI", 8.5F);
            lblVChasis.Location = new Point(435, 25);
            lblVChasis.Name = "lblVChasis";
            lblVChasis.Size = new Size(95, 15);
            lblVChasis.Text = "Chasis (17 car):";

            txtVChasis.Font = new Font("Segoe UI", 8.5F);
            txtVChasis.Location = new Point(530, 22);
            txtVChasis.Name = "txtVChasis";
            txtVChasis.PlaceholderText = "17 caracteres";
            txtVChasis.Size = new Size(140, 23);

            lblVAnio.Font = new Font("Segoe UI", 8.5F);
            lblVAnio.Location = new Point(10, 60);
            lblVAnio.Name = "lblVAnio";
            lblVAnio.Size = new Size(35, 15);
            lblVAnio.Text = "Año:";

            txtVAnio.Font = new Font("Segoe UI", 8.5F);
            txtVAnio.Location = new Point(55, 57);
            txtVAnio.Name = "txtVAnio";
            txtVAnio.PlaceholderText = "2024";
            txtVAnio.Size = new Size(60, 23);

            lblVColor.Font = new Font("Segoe UI", 8.5F);
            lblVColor.Location = new Point(125, 60);
            lblVColor.Name = "lblVColor";
            lblVColor.Size = new Size(40, 15);
            lblVColor.Text = "Color:";

            txtVColor.Font = new Font("Segoe UI", 8.5F);
            txtVColor.Location = new Point(165, 57);
            txtVColor.Name = "txtVColor";
            txtVColor.PlaceholderText = "Blanco";
            txtVColor.Size = new Size(90, 23);

            lblVPrecio.Font = new Font("Segoe UI", 8.5F);
            lblVPrecio.Location = new Point(265, 60);
            lblVPrecio.Name = "lblVPrecio";
            lblVPrecio.Size = new Size(45, 15);
            lblVPrecio.Text = "Precio:";

            txtVPrecio.Font = new Font("Segoe UI", 8.5F);
            txtVPrecio.Location = new Point(310, 57);
            txtVPrecio.Name = "txtVPrecio";
            txtVPrecio.PlaceholderText = "18500.00";
            txtVPrecio.Size = new Size(90, 23);

            // [Actividad 5] [Actividad 10 - P27] Botón de acción principal para disparar POST /api/vehiculos (evento btnCrearVehiculo_Click).
            btnCrearVehiculo.BackColor = Color.FromArgb(0, 120, 215);
            btnCrearVehiculo.ForeColor = Color.White;
            btnCrearVehiculo.Location = new Point(430, 54);
            btnCrearVehiculo.Name = "btnCrearVehiculo";
            btnCrearVehiculo.Size = new Size(160, 30);
            btnCrearVehiculo.TabIndex = 14;
            btnCrearVehiculo.Text = "+ Crear Vehículo";
            btnCrearVehiculo.UseVisualStyleBackColor = false;
            btnCrearVehiculo.Click += btnCrearVehiculo_Click;

            // [Actividad 5] [Actividad 10 - P27] Contenedor visual del catálogo de vehículos: filtro por marca, búsqueda por ID, estado HTTP, DataGridView y contador.
            // grpListarVehiculos
            grpListarVehiculos.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            grpListarVehiculos.Controls.Add(lblFiltroMarca);
            grpListarVehiculos.Controls.Add(txtFiltroMarca);
            grpListarVehiculos.Controls.Add(btnConsultar);
            grpListarVehiculos.Controls.Add(lblIdVehiculo);
            grpListarVehiculos.Controls.Add(txtIdVehiculo);
            grpListarVehiculos.Controls.Add(btnBuscarId);
            grpListarVehiculos.Controls.Add(btnLimpiarVehiculos);
            grpListarVehiculos.Controls.Add(lblEstado);
            grpListarVehiculos.Controls.Add(dgcVehiculos);
            grpListarVehiculos.Controls.Add(lblTotal);
            grpListarVehiculos.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            grpListarVehiculos.Location = new Point(10, 110);
            grpListarVehiculos.Name = "grpListarVehiculos";
            grpListarVehiculos.Size = new Size(915, 395);
            grpListarVehiculos.TabIndex = 1;
            grpListarVehiculos.TabStop = false;
            grpListarVehiculos.Text = "Catálogo y Consulta de Vehículos (GET /api/vehiculos)";

            // [Actividad 5] Controles de filtrado por marca y botón de consulta GET /api/vehiculos?marca=...
            lblFiltroMarca.Font = new Font("Segoe UI", 8.5F);
            lblFiltroMarca.Location = new Point(15, 25);
            lblFiltroMarca.Name = "lblFiltroMarca";
            lblFiltroMarca.Size = new Size(45, 15);
            lblFiltroMarca.Text = "Marca:";

            txtFiltroMarca.Font = new Font("Segoe UI", 8.5F);
            txtFiltroMarca.Location = new Point(65, 22);
            txtFiltroMarca.Name = "txtFiltroMarca";
            txtFiltroMarca.PlaceholderText = "Toyota...";
            txtFiltroMarca.Size = new Size(120, 23);

            btnConsultar.Location = new Point(195, 20);
            btnConsultar.Name = "btnConsultar";
            btnConsultar.Size = new Size(130, 26);
            btnConsultar.Text = "Listar / Filtrar";
            btnConsultar.Click += btnConsultar_Click;

            // [Actividad 5] [Actividad 10 - P27] Controles de búsqueda puntual por ID (GET /api/vehiculos/{id}) y botón de limpieza de vista.
            lblIdVehiculo.Font = new Font("Segoe UI", 8.5F);
            lblIdVehiculo.Location = new Point(340, 25);
            lblIdVehiculo.Name = "lblIdVehiculo";
            lblIdVehiculo.Size = new Size(70, 15);
            lblIdVehiculo.Text = "ID Vehículo:";

            txtIdVehiculo.Font = new Font("Segoe UI", 8.5F);
            txtIdVehiculo.Location = new Point(415, 22);
            txtIdVehiculo.Name = "txtIdVehiculo";
            txtIdVehiculo.PlaceholderText = "1";
            txtIdVehiculo.Size = new Size(50, 23);

            btnBuscarId.Location = new Point(475, 20);
            btnBuscarId.Name = "btnBuscarId";
            btnBuscarId.Size = new Size(110, 26);
            btnBuscarId.Text = "Buscar por ID";
            btnBuscarId.Click += btnBuscarId_Click;

            btnLimpiarVehiculos.Font = new Font("Segoe UI", 8.5F);
            btnLimpiarVehiculos.Location = new Point(595, 20);
            btnLimpiarVehiculos.Name = "btnLimpiarVehiculos";
            btnLimpiarVehiculos.Size = new Size(80, 26);
            btnLimpiarVehiculos.Text = "Limpiar";
            btnLimpiarVehiculos.Click += btnLimpiarVehiculos_Click;

            // [Actividad 5] [Actividad 11] Barra de estado visual que muestra éxito HTTP o rechazos controlados (400/401/403/404/409/5xx).
            lblEstado.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstado.ForeColor = Color.DimGray;
            lblEstado.Location = new Point(15, 52);
            lblEstado.Name = "lblEstado";
            lblEstado.Size = new Size(880, 18);
            lblEstado.Text = "Estado: En espera de consulta";

            // [Actividad 5] [Actividad 10 - P27] Grilla de solo lectura donde se despliegan los VehiculoDto y su columna Estado (DISPONIBLE/VENDIDO).
            dgcVehiculos.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            dgcVehiculos.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcVehiculos.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcVehiculos.Location = new Point(15, 75);
            dgcVehiculos.Name = "dgcVehiculos";
            dgcVehiculos.ReadOnly = true;
            dgcVehiculos.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcVehiculos.Size = new Size(885, 285);

            // [Actividad 5] Etiqueta de resumen con el total de vehículos recuperados en la última consulta.
            lblTotal.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
            lblTotal.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblTotal.Location = new Point(15, 368);
            lblTotal.Name = "lblTotal";
            lblTotal.Size = new Size(300, 15);
            lblTotal.Text = "Total: 0 vehículos";

            // ==========================================
            // [Actividad 5] [Actividad 10 - P27] TAB 2: COMPRADORES (POST / GET /api/compradores)
            // ==========================================
            tabCompradores.Controls.Add(grpCrearComprador);
            tabCompradores.Controls.Add(grpListarCompradores);
            tabCompradores.Location = new Point(4, 24);
            tabCompradores.Name = "tabCompradores";
            tabCompradores.Padding = new Padding(8);
            tabCompradores.Size = new Size(937, 512);
            tabCompradores.TabIndex = 1;
            tabCompradores.Text = "2. Compradores (POST / GET)";
            tabCompradores.UseVisualStyleBackColor = true;

            // [Actividad 5] [Actividad 10 - P27] Grupo visual para dar de alta compradores mediante POST /api/compradores (requiere OPERADOR/ADMIN).
            // grpCrearComprador
            grpCrearComprador.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            grpCrearComprador.Controls.Add(lblCDocumento);
            grpCrearComprador.Controls.Add(txtCDocumento);
            grpCrearComprador.Controls.Add(lblCNombre);
            grpCrearComprador.Controls.Add(txtCNombre);
            grpCrearComprador.Controls.Add(lblCCorreo);
            grpCrearComprador.Controls.Add(txtCCorreo);
            grpCrearComprador.Controls.Add(btnCrearComprador);
            grpCrearComprador.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            grpCrearComprador.Location = new Point(10, 8);
            grpCrearComprador.Name = "grpCrearComprador";
            grpCrearComprador.Size = new Size(915, 75);
            grpCrearComprador.TabIndex = 0;
            grpCrearComprador.TabStop = false;
            grpCrearComprador.Text = "Registrar Comprador (POST /api/compradores - Requiere OPERADOR o ADMIN)";

            // [Actividad 5] Campos del contrato CrearCompradorRequest (Documento, Nombre y Correo) y botón "+ Crear Comprador".
            lblCDocumento.Font = new Font("Segoe UI", 8.5F);
            lblCDocumento.Location = new Point(15, 30);
            lblCDocumento.Name = "lblCDocumento";
            lblCDocumento.Size = new Size(75, 15);
            lblCDocumento.Text = "Documento:";

            txtCDocumento.Font = new Font("Segoe UI", 8.5F);
            txtCDocumento.Location = new Point(95, 27);
            txtCDocumento.Name = "txtCDocumento";
            txtCDocumento.PlaceholderText = "1804567890";
            txtCDocumento.Size = new Size(120, 23);

            lblCNombre.Font = new Font("Segoe UI", 8.5F);
            lblCNombre.Location = new Point(230, 30);
            lblCNombre.Name = "lblCNombre";
            lblCNombre.Size = new Size(55, 15);
            lblCNombre.Text = "Nombre:";

            txtCNombre.Font = new Font("Segoe UI", 8.5F);
            txtCNombre.Location = new Point(290, 27);
            txtCNombre.Name = "txtCNombre";
            txtCNombre.PlaceholderText = "Juan Perez";
            txtCNombre.Size = new Size(180, 23);

            lblCCorreo.Font = new Font("Segoe UI", 8.5F);
            lblCCorreo.Location = new Point(485, 30);
            lblCCorreo.Name = "lblCCorreo";
            lblCCorreo.Size = new Size(50, 15);
            lblCCorreo.Text = "Correo:";

            txtCCorreo.Font = new Font("Segoe UI", 8.5F);
            txtCCorreo.Location = new Point(540, 27);
            txtCCorreo.Name = "txtCCorreo";
            txtCCorreo.PlaceholderText = "juan.perez@uta.edu";
            txtCCorreo.Size = new Size(190, 23);

            btnCrearComprador.BackColor = Color.FromArgb(0, 120, 215);
            btnCrearComprador.ForeColor = Color.White;
            btnCrearComprador.Location = new Point(745, 24);
            btnCrearComprador.Name = "btnCrearComprador";
            btnCrearComprador.Size = new Size(150, 28);
            btnCrearComprador.Text = "+ Crear Comprador";
            btnCrearComprador.UseVisualStyleBackColor = false;
            btnCrearComprador.Click += btnCrearComprador_Click;

            // [Actividad 5] [Actividad 11] Grupo visual para listar compradores (GET /api/compradores), mostrar estado HTTP y total de registros.
            // grpListarCompradores
            grpListarCompradores.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            grpListarCompradores.Controls.Add(btnListarCompradores);
            grpListarCompradores.Controls.Add(lblEstadoCompradores);
            grpListarCompradores.Controls.Add(dgcCompradores);
            grpListarCompradores.Controls.Add(lblTotalCompradores);
            grpListarCompradores.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            grpListarCompradores.Location = new Point(10, 95);
            grpListarCompradores.Name = "grpListarCompradores";
            grpListarCompradores.Size = new Size(915, 410);
            grpListarCompradores.TabIndex = 1;
            grpListarCompradores.TabStop = false;
            grpListarCompradores.Text = "Catálogo de Compradores (GET /api/compradores)";

            // [Actividad 5] Botón de consulta de compradores, etiqueta de estado HTTP, grilla de solo lectura y contador de compradores.
            btnListarCompradores.Location = new Point(15, 25);
            btnListarCompradores.Name = "btnListarCompradores";
            btnListarCompradores.Size = new Size(160, 28);
            btnListarCompradores.Text = "Listar Compradores";
            btnListarCompradores.Click += btnListarCompradores_Click;

            lblEstadoCompradores.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstadoCompradores.ForeColor = Color.DimGray;
            lblEstadoCompradores.Location = new Point(190, 30);
            lblEstadoCompradores.Name = "lblEstadoCompradores";
            lblEstadoCompradores.Size = new Size(700, 18);
            lblEstadoCompradores.Text = "Estado: En espera de consulta";

            dgcCompradores.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            dgcCompradores.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcCompradores.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcCompradores.Location = new Point(15, 60);
            dgcCompradores.Name = "dgcCompradores";
            dgcCompradores.ReadOnly = true;
            dgcCompradores.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcCompradores.Size = new Size(885, 310);

            lblTotalCompradores.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
            lblTotalCompradores.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblTotalCompradores.Location = new Point(15, 380);
            lblTotalCompradores.Name = "lblTotalCompradores";
            lblTotalCompradores.Size = new Size(300, 15);
            lblTotalCompradores.Text = "Total: 0 compradores";

            // ==========================================
            // [Actividad 5] [Actividad 10 - P27] TAB 3: VENTAS (SOLO CONSULTA - GET /api/ventas y GET /api/ventas/{id})
            // Prohibido exponer controles de creación (POST /api/ventas pertenece exclusivamente a la PWA móvil).
            // ==========================================
            tabVentas.Controls.Add(lblAvisoVentas);
            tabVentas.Controls.Add(lblIdVenta);
            tabVentas.Controls.Add(txtIdVenta);
            tabVentas.Controls.Add(btnConsultarVenta);
            tabVentas.Controls.Add(btnListarVentas);
            tabVentas.Controls.Add(lblEstadoVenta);
            tabVentas.Controls.Add(dgcVentas);
            tabVentas.Controls.Add(grpDetalleVenta);
            tabVentas.Location = new Point(4, 24);
            tabVentas.Name = "tabVentas";
            tabVentas.Padding = new Padding(8);
            tabVentas.Size = new Size(937, 512);
            tabVentas.TabIndex = 2;
            tabVentas.Text = "3. Ventas (GET - Consulta Exclusiva)";
            tabVentas.UseVisualStyleBackColor = true;

            // [Actividad 5] Aviso arquitectónico explícito que aclara que el registro de ventas (POST /api/ventas) es exclusivo de la PWA móvil.
            lblAvisoVentas.AutoSize = true;
            lblAvisoVentas.Font = new Font("Segoe UI", 8.5F, FontStyle.Italic);
            lblAvisoVentas.ForeColor = Color.DarkRed;
            lblAvisoVentas.Location = new Point(15, 8);
            lblAvisoVentas.Name = "lblAvisoVentas";
            lblAvisoVentas.Size = new Size(720, 15);
            lblAvisoVentas.Text = "Nota de Arquitectura: La PWA móvil es el cliente exclusivo para registrar ventas. Esta interfaz administrativa solo permite consulta y seguimiento.";

            // [Actividad 5] Barra superior de consulta de ventas: búsqueda individual por ID (GET /api/ventas/{id}), listado general (GET /api/ventas) y etiqueta de estado HTTP.
            lblIdVenta.AutoSize = true;
            lblIdVenta.Location = new Point(15, 33);
            lblIdVenta.Name = "lblIdVenta";
            lblIdVenta.Size = new Size(54, 15);
            lblIdVenta.Text = "ID Venta:";

            txtIdVenta.Location = new Point(75, 30);
            txtIdVenta.Name = "txtIdVenta";
            txtIdVenta.PlaceholderText = "Ej: 1";
            txtIdVenta.Size = new Size(65, 23);

            btnConsultarVenta.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            btnConsultarVenta.Location = new Point(150, 28);
            btnConsultarVenta.Name = "btnConsultarVenta";
            btnConsultarVenta.Size = new Size(130, 27);
            btnConsultarVenta.Text = "Buscar por ID";
            btnConsultarVenta.Click += btnConsultarVenta_Click;

            btnListarVentas.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            btnListarVentas.Location = new Point(290, 28);
            btnListarVentas.Name = "btnListarVentas";
            btnListarVentas.Size = new Size(160, 27);
            btnListarVentas.Text = "Listar Todas las Ventas";
            btnListarVentas.Click += btnListarVentas_Click;

            lblEstadoVenta.AutoSize = true;
            lblEstadoVenta.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstadoVenta.ForeColor = Color.DimGray;
            lblEstadoVenta.Location = new Point(465, 34);
            lblEstadoVenta.Name = "lblEstadoVenta";
            lblEstadoVenta.Size = new Size(400, 15);
            lblEstadoVenta.Text = "Estado: En espera de consulta";

            // [Actividad 5] [Actividad 10 - P27] Grilla de solo lectura del histórico de ventas con evento SelectionChanged para actualizar el panel de auditoría lateral.
            dgcVentas.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left;
            dgcVentas.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcVentas.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcVentas.Location = new Point(15, 65);
            dgcVentas.Name = "dgcVentas";
            dgcVentas.ReadOnly = true;
            dgcVentas.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcVentas.Size = new Size(550, 435);
            dgcVentas.SelectionChanged += dgcVentas_SelectionChanged;

            // [Actividad 5] [Actividad 10 - P27] Panel lateral de detalle individual de venta (GET /api/ventas/{id}) con los 9 campos auditables de VentaDto.
            // grpDetalleVenta
            grpDetalleVenta.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            grpDetalleVenta.Controls.Add(lblValorFecha);
            grpDetalleVenta.Controls.Add(lblEtiqFecha);
            grpDetalleVenta.Controls.Add(lblValorNotificacion);
            grpDetalleVenta.Controls.Add(lblEtiqNotif);
            grpDetalleVenta.Controls.Add(lblValorEstadoVenta);
            grpDetalleVenta.Controls.Add(lblEtiqEstadoVenta);
            grpDetalleVenta.Controls.Add(lblValorMoneda);
            grpDetalleVenta.Controls.Add(lblEtiqMoneda);
            grpDetalleVenta.Controls.Add(lblValorPrecio);
            grpDetalleVenta.Controls.Add(lblEtiqPrecio);
            grpDetalleVenta.Controls.Add(lblValorCompradorId);
            grpDetalleVenta.Controls.Add(lblEtiqCompradorId);
            grpDetalleVenta.Controls.Add(lblValorVehiculoId);
            grpDetalleVenta.Controls.Add(lblEtiqVehiculoId);
            grpDetalleVenta.Controls.Add(lblValorOperacionId);
            grpDetalleVenta.Controls.Add(lblEtiqOperacionId);
            grpDetalleVenta.Controls.Add(lblValorVentaId);
            grpDetalleVenta.Controls.Add(lblEtiqVentaId);
            grpDetalleVenta.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            grpDetalleVenta.Location = new Point(575, 60);
            grpDetalleVenta.Name = "grpDetalleVenta";
            grpDetalleVenta.Size = new Size(350, 440);
            grpDetalleVenta.TabIndex = 5;
            grpDetalleVenta.TabStop = false;
            grpDetalleVenta.Text = "Detalle Individual de Venta (GET /api/ventas/{id})";

            // [Actividad 5] [Actividad 10 - P27] Pares de etiquetas clave/valor del panel de detalle de venta (inicializados en "-" sin datos ficticios).
            lblEtiqVentaId.Location = new Point(15, 30);
            lblEtiqVentaId.Text = "ID Venta:";
            lblValorVentaId.Location = new Point(130, 30);
            lblValorVentaId.Text = "-";

            lblEtiqOperacionId.Location = new Point(15, 60);
            lblEtiqOperacionId.Text = "Operación UUID:";
            lblValorOperacionId.Location = new Point(130, 60);
            lblValorOperacionId.Size = new Size(210, 30);
            lblValorOperacionId.Text = "-";

            lblEtiqVehiculoId.Location = new Point(15, 100);
            lblEtiqVehiculoId.Text = "ID Vehículo:";
            lblValorVehiculoId.Location = new Point(130, 100);
            lblValorVehiculoId.Text = "-";

            lblEtiqCompradorId.Location = new Point(15, 130);
            lblEtiqCompradorId.Text = "ID Comprador:";
            lblValorCompradorId.Location = new Point(130, 130);
            lblValorCompradorId.Text = "-";

            lblEtiqPrecio.Location = new Point(15, 160);
            lblEtiqPrecio.Text = "Precio Acordado:";
            lblValorPrecio.Location = new Point(130, 160);
            lblValorPrecio.Text = "-";

            lblEtiqMoneda.Location = new Point(15, 190);
            lblEtiqMoneda.Text = "Moneda:";
            lblValorMoneda.Location = new Point(130, 190);
            lblValorMoneda.Text = "-";

            lblEtiqEstadoVenta.Location = new Point(15, 220);
            lblEtiqEstadoVenta.Text = "Estado Venta:";
            lblValorEstadoVenta.Location = new Point(130, 220);
            lblValorEstadoVenta.Text = "-";

            lblEtiqNotif.Location = new Point(15, 250);
            lblEtiqNotif.Text = "Notificación:";
            lblValorNotificacion.Location = new Point(130, 250);
            lblValorNotificacion.Text = "-";

            lblEtiqFecha.Location = new Point(15, 280);
            lblEtiqFecha.Text = "Fecha UTC:";
            lblValorFecha.Location = new Point(130, 280);
            lblValorFecha.Size = new Size(210, 30);
            lblValorFecha.Text = "-";

            // [Actividad 5] Configuración general de la ventana principal Form1: tamaño, centrado, dimensiones mínimas y registro de controles raíz.
            // Form1
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(990, 645);
            Controls.Add(tabControlPrincipal);
            Controls.Add(txtToken);
            Controls.Add(lblToken);
            Controls.Add(lblSubtitulo);
            Controls.Add(lblTitulo);
            MinimumSize = new Size(900, 600);
            Name = "Form1";
            StartPosition = FormStartPosition.CenterScreen;
            Text = "Administración Multicanal SOA - Windows Forms (.NET 8)";

            // [Actividad 5] Reanudación del motor de diseño y finalización de inicialización de los DataGridView en las tres pestañas.
            tabControlPrincipal.ResumeLayout(false);
            tabVehiculos.ResumeLayout(false);
            grpCrearVehiculo.ResumeLayout(false);
            grpCrearVehiculo.PerformLayout();
            grpListarVehiculos.ResumeLayout(false);
            grpListarVehiculos.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)dgcVehiculos).EndInit();

            tabCompradores.ResumeLayout(false);
            grpCrearComprador.ResumeLayout(false);
            grpCrearComprador.PerformLayout();
            grpListarCompradores.ResumeLayout(false);
            ((System.ComponentModel.ISupportInitialize)dgcCompradores).EndInit();

            tabVentas.ResumeLayout(false);
            tabVentas.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)dgcVentas).EndInit();
            grpDetalleVenta.ResumeLayout(false);
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        // ==========================================
        // [Actividad 5] [Actividad 11] Controles de cabecera global y contenedor de pestañas
        // ==========================================
        // [Actividad 5] Título principal del cliente administrativo.
        private Label lblTitulo;
        // [Actividad 5] Subtítulo de arquitectura con la ruta del Gateway NGINX (:8090).
        private Label lblSubtitulo;
        // [Actividad 5] [Actividad 11] Etiqueta del campo de autenticación JWT.
        private Label lblToken;
        // [Actividad 5] [Actividad 11] Campo compartido donde el operador ingresa el Bearer JWT.
        private TextBox txtToken;
        // [Actividad 5] Control de pestañas que aloja Vehículos, Compradores y Ventas (solo consulta).
        private TabControl tabControlPrincipal;

        // ==========================================
        // [Actividad 5] [Actividad 10 - P27] Controles de Pestaña 1: Vehículos (POST / GET /api/vehiculos)
        // ==========================================
        // [Actividad 5] Pestaña administrativa del catálogo de vehículos (Java Spring Boot vía NGINX :8090).
        private TabPage tabVehiculos;
        // [Actividad 5] Grupo visual para el alta de un nuevo vehículo (POST /api/vehiculos).
        private GroupBox grpCrearVehiculo;
        // [Actividad 5] Etiqueta y campo para la marca del nuevo vehículo.
        private Label lblVMarga;
        private TextBox txtVMarca;
        // [Actividad 5] Etiqueta y campo para el modelo del nuevo vehículo.
        private Label lblVModelo;
        private TextBox txtVModelo;
        // [Actividad 5] Etiqueta y campo para la placa única del vehículo.
        private Label lblVPlaca;
        private TextBox txtVPlaca;
        // [Actividad 5] Etiqueta y campo para el chasis (VIN de 17 caracteres).
        private Label lblVChasis;
        private TextBox txtVChasis;
        // [Actividad 5] Etiqueta y campo para el año de fabricación del vehículo.
        private Label lblVAnio;
        private TextBox txtVAnio;
        // [Actividad 5] Etiqueta y campo para el color del vehículo.
        private Label lblVColor;
        private TextBox txtVColor;
        // [Actividad 5] Etiqueta y campo para el precio monetario del vehículo.
        private Label lblVPrecio;
        private TextBox txtVPrecio;
        // [Actividad 5] Botón que dispara el alta administrativa POST /api/vehiculos.
        private Button btnCrearVehiculo;

        // [Actividad 5] [Actividad 10 - P27] Grupo visual para listar, filtrar por marca y buscar vehículos por ID (auditoría de Estado VENDIDO).
        private GroupBox grpListarVehiculos;
        // [Actividad 5] Etiqueta y campo de texto para filtrar por marca (?marca=...).
        private Label lblFiltroMarca;
        private TextBox txtFiltroMarca;
        // [Actividad 5] Botón para ejecutar GET /api/vehiculos (completo o filtrado por marca).
        private Button btnConsultar;
        // [Actividad 5] Etiqueta y campo numérico para consultar un vehículo específico por ID.
        private Label lblIdVehiculo;
        private TextBox txtIdVehiculo;
        // [Actividad 5] Botón para ejecutar GET /api/vehiculos/{id}.
        private Button btnBuscarId;
        // [Actividad 5] Botón para limpiar filtros, grilla y estado de la pestaña Vehículos.
        private Button btnLimpiarVehiculos;
        // [Actividad 5] [Actividad 11] Etiqueta de retroalimentación visual de códigos HTTP en Vehículos.
        private Label lblEstado;
        // [Actividad 5] [Actividad 10 - P27] Grilla tabular de solo lectura que muestra VehiculoDto.
        private DataGridView dgcVehiculos;
        // [Actividad 5] Contador total de vehículos visualizados en dgcVehiculos.
        private Label lblTotal;

        // ==========================================
        // [Actividad 5] [Actividad 10 - P27] Controles de Pestaña 2: Compradores (POST / GET /api/compradores)
        // ==========================================
        // [Actividad 5] Pestaña administrativa de compradores (Python FastAPI vía NGINX :8090).
        private TabPage tabCompradores;
        // [Actividad 5] Grupo visual para registrar nuevos compradores (POST /api/compradores).
        private GroupBox grpCrearComprador;
        // [Actividad 5] Etiqueta y campo para el documento de identidad del comprador.
        private Label lblCDocumento;
        private TextBox txtCDocumento;
        // [Actividad 5] Etiqueta y campo para el nombre completo del comprador.
        private Label lblCNombre;
        private TextBox txtCNombre;
        // [Actividad 5] Etiqueta y campo para el correo electrónico del comprador.
        private Label lblCCorreo;
        private TextBox txtCCorreo;
        // [Actividad 5] Botón que dispara el registro POST /api/compradores.
        private Button btnCrearComprador;

        // [Actividad 5] Grupo visual para listar el catálogo de compradores (GET /api/compradores).
        private GroupBox grpListarCompradores;
        // [Actividad 5] Botón que dispara la consulta GET /api/compradores.
        private Button btnListarCompradores;
        // [Actividad 5] [Actividad 11] Etiqueta de estado HTTP para operaciones de Compradores.
        private Label lblEstadoCompradores;
        // [Actividad 5] Grilla de solo lectura que despliega los CompradorDto recuperados.
        private DataGridView dgcCompradores;
        // [Actividad 5] Contador total de compradores cargados en la grilla.
        private Label lblTotalCompradores;

        // ==========================================
        // [Actividad 5] [Actividad 10 - P27] Controles de Pestaña 3: Ventas (SOLO CONSULTA - GET /api/ventas y GET /api/ventas/{id})
        // ==========================================
        // [Actividad 5] Pestaña de seguimiento exclusivo de ventas (prohíbe POST /api/ventas).
        private TabPage tabVentas;
        // [Actividad 5] Etiqueta de advertencia arquitectónica sobre la exclusividad de la PWA para registrar ventas.
        private Label lblAvisoVentas;
        // [Actividad 5] Etiqueta y campo numérico para ingresar el ID de venta a auditar.
        private Label lblIdVenta;
        private TextBox txtIdVenta;
        // [Actividad 5] Botón que consulta una venta individual mediante GET /api/ventas/{id}.
        private Button btnConsultarVenta;
        // [Actividad 5] Botón que consulta el histórico completo mediante GET /api/ventas.
        private Button btnListarVentas;
        // [Actividad 5] [Actividad 11] Etiqueta de estado HTTP para las consultas de Ventas.
        private Label lblEstadoVenta;
        // [Actividad 5] [Actividad 10 - P27] Grilla de solo lectura con el listado histórico de VentaDto.
        private DataGridView dgcVentas;
        // [Actividad 5] [Actividad 10 - P27] Panel de auditoría detallada de la venta consultada o seleccionada.
        private GroupBox grpDetalleVenta;
        // [Actividad 5] Etiquetas de valor y rótulo para la fecha UTC de la venta.
        private Label lblValorFecha;
        private Label lblEtiqFecha;
        // [Actividad 5] [Actividad 10 - P27] Etiquetas de valor y rótulo para el estado de la notificación.
        private Label lblValorNotificacion;
        private Label lblEtiqNotif;
        // [Actividad 5] [Actividad 10 - P27] Etiquetas de valor y rótulo para el estado de la venta (COMPLETADA/EN_REVISION/FALLIDA).
        private Label lblValorEstadoVenta;
        private Label lblEtiqEstadoVenta;
        // [Actividad 5] Etiquetas de valor y rótulo para la moneda de la transacción.
        private Label lblValorMoneda;
        private Label lblEtiqMoneda;
        // [Actividad 5] Etiquetas de valor y rótulo para el precio acordado de la venta.
        private Label lblValorPrecio;
        private Label lblEtiqPrecio;
        // [Actividad 5] [Actividad 10 - P27] Etiquetas de valor y rótulo para el ID del comprador asociado.
        private Label lblValorCompradorId;
        private Label lblEtiqCompradorId;
        // [Actividad 5] [Actividad 10 - P27] Etiquetas de valor y rótulo para el ID del vehículo vendido.
        private Label lblValorVehiculoId;
        private Label lblEtiqVehiculoId;
        // [Actividad 5] [Actividad 10 - P27] Etiquetas de valor y rótulo para el UUID de operación distribuida.
        private Label lblValorOperacionId;
        private Label lblEtiqOperacionId;
        // [Actividad 5] Etiquetas de valor y rótulo para el identificador numérico de la venta.
        private Label lblValorVentaId;
        private Label lblEtiqVentaId;
    }
}
