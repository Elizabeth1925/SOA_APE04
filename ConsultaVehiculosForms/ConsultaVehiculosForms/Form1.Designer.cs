namespace ConsultaVehiculosForms
{
    partial class Form1
    {
        private System.ComponentModel.IContainer components = null;

        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        private void InitializeComponent()
        {
            lblTitulo = new Label();
            lblSubtitulo = new Label();
            lblToken = new Label();
            txtToken = new TextBox();
            tabControlPrincipal = new TabControl();
            
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

            grpListarCompradores = new GroupBox();
            btnListarCompradores = new Button();
            lblEstadoCompradores = new Label();
            dgcCompradores = new DataGridView();
            lblTotalCompradores = new Label();

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

            // lblTitulo
            lblTitulo.AutoSize = true;
            lblTitulo.Font = new Font("Segoe UI", 13F, FontStyle.Bold);
            lblTitulo.Location = new Point(25, 12);
            lblTitulo.Name = "lblTitulo";
            lblTitulo.Size = new Size(540, 25);
            lblTitulo.TabIndex = 0;
            lblTitulo.Text = "SOA APE 04 - Administración Multicanal (Windows Forms)";

            // lblSubtitulo
            lblSubtitulo.AutoSize = true;
            lblSubtitulo.ForeColor = Color.DarkSlateGray;
            lblSubtitulo.Location = new Point(26, 38);
            lblSubtitulo.Name = "lblSubtitulo";
            lblSubtitulo.Size = new Size(620, 15);
            lblSubtitulo.TabIndex = 1;
            lblSubtitulo.Text = "Conectado al Gateway NGINX (:8090) hacia Spring Boot (:8080/:8082) y Python FastAPI (:8002)";

            // lblToken
            lblToken.AutoSize = true;
            lblToken.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblToken.Location = new Point(26, 62);
            lblToken.Name = "lblToken";
            lblToken.Size = new Size(69, 15);
            lblToken.TabIndex = 2;
            lblToken.Text = "Token JWT:";

            // txtToken
            txtToken.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtToken.Location = new Point(101, 59);
            txtToken.Name = "txtToken";
            txtToken.PlaceholderText = "Pegue aquí el Bearer Token JWT (obtenido en /oauth/token)";
            txtToken.Size = new Size(865, 23);
            txtToken.TabIndex = 3;

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
            // TAB 1: VEHICULOS
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

            btnCrearVehiculo.BackColor = Color.FromArgb(0, 120, 215);
            btnCrearVehiculo.ForeColor = Color.White;
            btnCrearVehiculo.Location = new Point(430, 54);
            btnCrearVehiculo.Name = "btnCrearVehiculo";
            btnCrearVehiculo.Size = new Size(160, 30);
            btnCrearVehiculo.TabIndex = 14;
            btnCrearVehiculo.Text = "+ Crear Vehículo";
            btnCrearVehiculo.UseVisualStyleBackColor = false;
            btnCrearVehiculo.Click += btnCrearVehiculo_Click;

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

            lblEstado.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstado.ForeColor = Color.DimGray;
            lblEstado.Location = new Point(15, 52);
            lblEstado.Name = "lblEstado";
            lblEstado.Size = new Size(880, 18);
            lblEstado.Text = "Estado: En espera de consulta";

            dgcVehiculos.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            dgcVehiculos.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcVehiculos.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcVehiculos.Location = new Point(15, 75);
            dgcVehiculos.Name = "dgcVehiculos";
            dgcVehiculos.ReadOnly = true;
            dgcVehiculos.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcVehiculos.Size = new Size(885, 285);

            lblTotal.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
            lblTotal.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblTotal.Location = new Point(15, 368);
            lblTotal.Name = "lblTotal";
            lblTotal.Size = new Size(300, 15);
            lblTotal.Text = "Total: 0 vehículos";

            // ==========================================
            // TAB 2: COMPRADORES
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
            // TAB 3: VENTAS
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

            lblAvisoVentas.AutoSize = true;
            lblAvisoVentas.Font = new Font("Segoe UI", 8.5F, FontStyle.Italic);
            lblAvisoVentas.ForeColor = Color.DarkRed;
            lblAvisoVentas.Location = new Point(15, 8);
            lblAvisoVentas.Name = "lblAvisoVentas";
            lblAvisoVentas.Size = new Size(720, 15);
            lblAvisoVentas.Text = "Nota de Arquitectura: La PWA móvil es el cliente exclusivo para registrar ventas. Esta interfaz administrativa solo permite consulta y seguimiento.";

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

            dgcVentas.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left;
            dgcVentas.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcVentas.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcVentas.Location = new Point(15, 65);
            dgcVentas.Name = "dgcVentas";
            dgcVentas.ReadOnly = true;
            dgcVentas.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcVentas.Size = new Size(550, 435);
            dgcVentas.SelectionChanged += dgcVentas_SelectionChanged;

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

        private Label lblTitulo;
        private Label lblSubtitulo;
        private Label lblToken;
        private TextBox txtToken;
        private TabControl tabControlPrincipal;

        // Tab Vehiculos
        private TabPage tabVehiculos;
        private GroupBox grpCrearVehiculo;
        private Label lblVMarga;
        private TextBox txtVMarca;
        private Label lblVModelo;
        private TextBox txtVModelo;
        private Label lblVPlaca;
        private TextBox txtVPlaca;
        private Label lblVChasis;
        private TextBox txtVChasis;
        private Label lblVAnio;
        private TextBox txtVAnio;
        private Label lblVColor;
        private TextBox txtVColor;
        private Label lblVPrecio;
        private TextBox txtVPrecio;
        private Button btnCrearVehiculo;

        private GroupBox grpListarVehiculos;
        private Label lblFiltroMarca;
        private TextBox txtFiltroMarca;
        private Button btnConsultar;
        private Label lblIdVehiculo;
        private TextBox txtIdVehiculo;
        private Button btnBuscarId;
        private Button btnLimpiarVehiculos;
        private Label lblEstado;
        private DataGridView dgcVehiculos;
        private Label lblTotal;

        // Tab Compradores
        private TabPage tabCompradores;
        private GroupBox grpCrearComprador;
        private Label lblCDocumento;
        private TextBox txtCDocumento;
        private Label lblCNombre;
        private TextBox txtCNombre;
        private Label lblCCorreo;
        private TextBox txtCCorreo;
        private Button btnCrearComprador;

        private GroupBox grpListarCompradores;
        private Button btnListarCompradores;
        private Label lblEstadoCompradores;
        private DataGridView dgcCompradores;
        private Label lblTotalCompradores;

        // Tab Ventas
        private TabPage tabVentas;
        private Label lblAvisoVentas;
        private Label lblIdVenta;
        private TextBox txtIdVenta;
        private Button btnConsultarVenta;
        private Button btnListarVentas;
        private Label lblEstadoVenta;
        private DataGridView dgcVentas;
        private GroupBox grpDetalleVenta;
        private Label lblValorFecha;
        private Label lblEtiqFecha;
        private Label lblValorNotificacion;
        private Label lblEtiqNotif;
        private Label lblValorEstadoVenta;
        private Label lblEtiqEstadoVenta;
        private Label lblValorMoneda;
        private Label lblEtiqMoneda;
        private Label lblValorPrecio;
        private Label lblEtiqPrecio;
        private Label lblValorCompradorId;
        private Label lblEtiqCompradorId;
        private Label lblValorVehiculoId;
        private Label lblEtiqVehiculoId;
        private Label lblValorOperacionId;
        private Label lblEtiqOperacionId;
        private Label lblValorVentaId;
        private Label lblEtiqVentaId;
    }
}
