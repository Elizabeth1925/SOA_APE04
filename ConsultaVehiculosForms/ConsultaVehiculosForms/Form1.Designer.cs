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
            tabVehiculos = new TabPage();
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
            tabVentas = new TabPage();
            lblIdVenta = new Label();
            txtIdVenta = new TextBox();
            btnConsultarVenta = new Button();
            lblEstadoVenta = new Label();
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
            ((System.ComponentModel.ISupportInitialize)dgcVehiculos).BeginInit();
            tabVentas.SuspendLayout();
            grpDetalleVenta.SuspendLayout();
            SuspendLayout();
            // 
            // lblTitulo
            // 
            lblTitulo.AutoSize = true;
            lblTitulo.Font = new Font("Segoe UI", 13F, FontStyle.Bold);
            lblTitulo.Location = new Point(25, 15);
            lblTitulo.Name = "lblTitulo";
            lblTitulo.Size = new Size(420, 25);
            lblTitulo.TabIndex = 0;
            lblTitulo.Text = "SOA - Cliente Consumidor de Microservicios";
            // 
            // lblSubtitulo
            // 
            lblSubtitulo.AutoSize = true;
            lblSubtitulo.ForeColor = Color.DarkSlateGray;
            lblSubtitulo.Location = new Point(26, 42);
            lblSubtitulo.Name = "lblSubtitulo";
            lblSubtitulo.Size = new Size(440, 15);
            lblSubtitulo.TabIndex = 1;
            lblSubtitulo.Text = "Catálogo de Vehículos (Java :8080) y Consulta de Ventas (Python :8002)";
            // 
            // lblToken
            // 
            lblToken.AutoSize = true;
            lblToken.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblToken.Location = new Point(26, 73);
            lblToken.Name = "lblToken";
            lblToken.Size = new Size(69, 15);
            lblToken.TabIndex = 2;
            lblToken.Text = "Token JWT:";
            // 
            // txtToken
            // 
            txtToken.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
            txtToken.Location = new Point(101, 70);
            txtToken.Name = "txtToken";
            txtToken.PlaceholderText = "Pegue aquí el Bearer Token JWT (ej. obtenido en /oauth/token de Java)";
            txtToken.Size = new Size(743, 23);
            txtToken.TabIndex = 3;
            // 
            // tabControlPrincipal
            // 
            tabControlPrincipal.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            tabControlPrincipal.Controls.Add(tabVehiculos);
            tabControlPrincipal.Controls.Add(tabVentas);
            tabControlPrincipal.Location = new Point(25, 105);
            tabControlPrincipal.Name = "tabControlPrincipal";
            tabControlPrincipal.SelectedIndex = 0;
            tabControlPrincipal.Size = new Size(823, 445);
            tabControlPrincipal.TabIndex = 4;
            // 
            // tabVehiculos
            // 
            tabVehiculos.Controls.Add(lblFiltroMarca);
            tabVehiculos.Controls.Add(txtFiltroMarca);
            tabVehiculos.Controls.Add(btnConsultar);
            tabVehiculos.Controls.Add(lblIdVehiculo);
            tabVehiculos.Controls.Add(txtIdVehiculo);
            tabVehiculos.Controls.Add(btnBuscarId);
            tabVehiculos.Controls.Add(btnLimpiarVehiculos);
            tabVehiculos.Controls.Add(lblEstado);
            tabVehiculos.Controls.Add(dgcVehiculos);
            tabVehiculos.Controls.Add(lblTotal);
            tabVehiculos.Location = new Point(4, 24);
            tabVehiculos.Name = "tabVehiculos";
            tabVehiculos.Padding = new Padding(10);
            tabVehiculos.Size = new Size(815, 417);
            tabVehiculos.TabIndex = 0;
            tabVehiculos.Text = "Catálogo de Vehículos (Java :8080)";
            tabVehiculos.UseVisualStyleBackColor = true;
            // 
            // lblFiltroMarca
            // 
            lblFiltroMarca.AutoSize = true;
            lblFiltroMarca.Location = new Point(15, 16);
            lblFiltroMarca.Name = "lblFiltroMarca";
            lblFiltroMarca.Size = new Size(43, 15);
            lblFiltroMarca.TabIndex = 0;
            lblFiltroMarca.Text = "Marca:";
            // 
            // txtFiltroMarca
            // 
            txtFiltroMarca.Location = new Point(64, 13);
            txtFiltroMarca.Name = "txtFiltroMarca";
            txtFiltroMarca.PlaceholderText = "Ej: Toyota, Chevrolet...";
            txtFiltroMarca.Size = new Size(130, 23);
            txtFiltroMarca.TabIndex = 1;
            // 
            // btnConsultar
            // 
            btnConsultar.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            btnConsultar.Location = new Point(205, 12);
            btnConsultar.Name = "btnConsultar";
            btnConsultar.Size = new Size(135, 26);
            btnConsultar.TabIndex = 2;
            btnConsultar.Text = "Listar / Filtrar";
            btnConsultar.UseVisualStyleBackColor = true;
            btnConsultar.Click += btnConsultar_Click;
            // 
            // lblIdVehiculo
            // 
            lblIdVehiculo.AutoSize = true;
            lblIdVehiculo.Location = new Point(365, 16);
            lblIdVehiculo.Name = "lblIdVehiculo";
            lblIdVehiculo.Size = new Size(69, 15);
            lblIdVehiculo.TabIndex = 3;
            lblIdVehiculo.Text = "ID Vehículo:";
            // 
            // txtIdVehiculo
            // 
            txtIdVehiculo.Location = new Point(440, 13);
            txtIdVehiculo.Name = "txtIdVehiculo";
            txtIdVehiculo.PlaceholderText = "Ej: 1";
            txtIdVehiculo.Size = new Size(60, 23);
            txtIdVehiculo.TabIndex = 4;
            // 
            // btnBuscarId
            // 
            btnBuscarId.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            btnBuscarId.Location = new Point(510, 12);
            btnBuscarId.Name = "btnBuscarId";
            btnBuscarId.Size = new Size(110, 26);
            btnBuscarId.TabIndex = 5;
            btnBuscarId.Text = "Buscar por ID";
            btnBuscarId.UseVisualStyleBackColor = true;
            btnBuscarId.Click += btnBuscarId_Click;
            // 
            // btnLimpiarVehiculos
            // 
            btnLimpiarVehiculos.Location = new Point(630, 12);
            btnLimpiarVehiculos.Name = "btnLimpiarVehiculos";
            btnLimpiarVehiculos.Size = new Size(80, 26);
            btnLimpiarVehiculos.TabIndex = 6;
            btnLimpiarVehiculos.Text = "Limpiar";
            btnLimpiarVehiculos.UseVisualStyleBackColor = true;
            btnLimpiarVehiculos.Click += btnLimpiarVehiculos_Click;
            // 
            // lblEstado
            // 
            lblEstado.AutoSize = true;
            lblEstado.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstado.ForeColor = Color.DimGray;
            lblEstado.Location = new Point(15, 48);
            lblEstado.Name = "lblEstado";
            lblEstado.Size = new Size(160, 15);
            lblEstado.TabIndex = 7;
            lblEstado.Text = "Estado: En espera de consulta";
            // 
            // dgcVehiculos
            // 
            dgcVehiculos.Anchor = AnchorStyles.Top | AnchorStyles.Bottom | AnchorStyles.Left | AnchorStyles.Right;
            dgcVehiculos.AutoSizeColumnsMode = DataGridViewAutoSizeColumnsMode.Fill;
            dgcVehiculos.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            dgcVehiculos.Location = new Point(15, 75);
            dgcVehiculos.Name = "dgcVehiculos";
            dgcVehiculos.ReadOnly = true;
            dgcVehiculos.SelectionMode = DataGridViewSelectionMode.FullRowSelect;
            dgcVehiculos.Size = new Size(785, 300);
            dgcVehiculos.TabIndex = 8;
            // 
            // lblTotal
            // 
            lblTotal.Anchor = AnchorStyles.Bottom | AnchorStyles.Left;
            lblTotal.AutoSize = true;
            lblTotal.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblTotal.Location = new Point(15, 388);
            lblTotal.Name = "lblTotal";
            lblTotal.Size = new Size(106, 15);
            lblTotal.TabIndex = 9;
            lblTotal.Text = "Total: 0 vehículos";
            // 
            // tabVentas
            // 
            tabVentas.Controls.Add(lblIdVenta);
            tabVentas.Controls.Add(txtIdVenta);
            tabVentas.Controls.Add(btnConsultarVenta);
            tabVentas.Controls.Add(lblEstadoVenta);
            tabVentas.Controls.Add(grpDetalleVenta);
            tabVentas.Location = new Point(4, 24);
            tabVentas.Name = "tabVentas";
            tabVentas.Padding = new Padding(10);
            tabVentas.Size = new Size(815, 417);
            tabVentas.TabIndex = 1;
            tabVentas.Text = "Vista de Venta (Python :8002)";
            tabVentas.UseVisualStyleBackColor = true;
            // 
            // lblIdVenta
            // 
            lblIdVenta.AutoSize = true;
            lblIdVenta.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            lblIdVenta.Location = new Point(20, 20);
            lblIdVenta.Name = "lblIdVenta";
            lblIdVenta.Size = new Size(72, 15);
            lblIdVenta.TabIndex = 0;
            lblIdVenta.Text = "ID de Venta:";
            // 
            // txtIdVenta
            // 
            txtIdVenta.Location = new Point(100, 17);
            txtIdVenta.Name = "txtIdVenta";
            txtIdVenta.PlaceholderText = "Ej: 1, 2, 3...";
            txtIdVenta.Size = new Size(100, 23);
            txtIdVenta.TabIndex = 1;
            // 
            // btnConsultarVenta
            // 
            btnConsultarVenta.Font = new Font("Segoe UI", 9F, FontStyle.Bold);
            btnConsultarVenta.Location = new Point(215, 15);
            btnConsultarVenta.Name = "btnConsultarVenta";
            btnConsultarVenta.Size = new Size(130, 26);
            btnConsultarVenta.TabIndex = 2;
            btnConsultarVenta.Text = "Consultar Venta";
            btnConsultarVenta.UseVisualStyleBackColor = true;
            btnConsultarVenta.Click += btnConsultarVenta_Click;
            // 
            // lblEstadoVenta
            // 
            lblEstadoVenta.AutoSize = true;
            lblEstadoVenta.Font = new Font("Segoe UI", 9F, FontStyle.Italic);
            lblEstadoVenta.ForeColor = Color.DimGray;
            lblEstadoVenta.Location = new Point(20, 52);
            lblEstadoVenta.Name = "lblEstadoVenta";
            lblEstadoVenta.Size = new Size(160, 15);
            lblEstadoVenta.TabIndex = 3;
            lblEstadoVenta.Text = "Estado: En espera de consulta";
            // 
            // grpDetalleVenta
            // 
            grpDetalleVenta.Anchor = AnchorStyles.Top | AnchorStyles.Left | AnchorStyles.Right;
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
            grpDetalleVenta.Font = new Font("Segoe UI", 10F, FontStyle.Bold);
            grpDetalleVenta.Location = new Point(20, 85);
            grpDetalleVenta.Name = "grpDetalleVenta";
            grpDetalleVenta.Size = new Size(775, 290);
            grpDetalleVenta.TabIndex = 4;
            grpDetalleVenta.TabStop = false;
            grpDetalleVenta.Text = "Información de la Venta (GET /api/ventas/{id})";
            // 
            // lblValorFecha
            // 
            lblValorFecha.AutoSize = true;
            lblValorFecha.Font = new Font("Segoe UI", 9.5F);
            lblValorFecha.Location = new Point(500, 115);
            lblValorFecha.Name = "lblValorFecha";
            lblValorFecha.Size = new Size(13, 17);
            lblValorFecha.TabIndex = 17;
            lblValorFecha.Text = "-";
            // 
            // lblEtiqFecha
            // 
            lblEtiqFecha.AutoSize = true;
            lblEtiqFecha.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqFecha.Location = new Point(390, 115);
            lblEtiqFecha.Name = "lblEtiqFecha";
            lblEtiqFecha.Size = new Size(74, 17);
            lblEtiqFecha.TabIndex = 16;
            lblEtiqFecha.Text = "Fecha UTC:";
            // 
            // lblValorNotificacion
            // 
            lblValorNotificacion.AutoSize = true;
            lblValorNotificacion.Font = new Font("Segoe UI", 11F, FontStyle.Bold);
            lblValorNotificacion.ForeColor = Color.DarkSlateGray;
            lblValorNotificacion.Location = new Point(500, 75);
            lblValorNotificacion.Name = "lblValorNotificacion";
            lblValorNotificacion.Size = new Size(15, 20);
            lblValorNotificacion.TabIndex = 15;
            lblValorNotificacion.Text = "-";
            // 
            // lblEtiqNotif
            // 
            lblEtiqNotif.AutoSize = true;
            lblEtiqNotif.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqNotif.Location = new Point(390, 77);
            lblEtiqNotif.Name = "lblEtiqNotif";
            lblEtiqNotif.Size = new Size(88, 17);
            lblEtiqNotif.TabIndex = 14;
            lblEtiqNotif.Text = "Notificación:";
            // 
            // lblValorEstadoVenta
            // 
            lblValorEstadoVenta.AutoSize = true;
            lblValorEstadoVenta.Font = new Font("Segoe UI", 12F, FontStyle.Bold);
            lblValorEstadoVenta.ForeColor = Color.DarkSlateGray;
            lblValorEstadoVenta.Location = new Point(500, 36);
            lblValorEstadoVenta.Name = "lblValorEstadoVenta";
            lblValorEstadoVenta.Size = new Size(16, 21);
            lblValorEstadoVenta.TabIndex = 13;
            lblValorEstadoVenta.Text = "-";
            // 
            // lblEtiqEstadoVenta
            // 
            lblEtiqEstadoVenta.AutoSize = true;
            lblEtiqEstadoVenta.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqEstadoVenta.Location = new Point(390, 39);
            lblEtiqEstadoVenta.Name = "lblEtiqEstadoVenta";
            lblEtiqEstadoVenta.Size = new Size(53, 17);
            lblEtiqEstadoVenta.TabIndex = 12;
            lblEtiqEstadoVenta.Text = "Estado:";
            // 
            // lblValorMoneda
            // 
            lblValorMoneda.AutoSize = true;
            lblValorMoneda.Font = new Font("Segoe UI", 9.5F);
            lblValorMoneda.Location = new Point(140, 230);
            lblValorMoneda.Name = "lblValorMoneda";
            lblValorMoneda.Size = new Size(13, 17);
            lblValorMoneda.TabIndex = 11;
            lblValorMoneda.Text = "-";
            // 
            // lblEtiqMoneda
            // 
            lblEtiqMoneda.AutoSize = true;
            lblEtiqMoneda.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqMoneda.Location = new Point(25, 230);
            lblEtiqMoneda.Name = "lblEtiqMoneda";
            lblEtiqMoneda.Size = new Size(62, 17);
            lblEtiqMoneda.TabIndex = 10;
            lblEtiqMoneda.Text = "Moneda:";
            // 
            // lblValorPrecio
            // 
            lblValorPrecio.AutoSize = true;
            lblValorPrecio.Font = new Font("Segoe UI", 9.5F);
            lblValorPrecio.Location = new Point(140, 192);
            lblValorPrecio.Name = "lblValorPrecio";
            lblValorPrecio.Size = new Size(13, 17);
            lblValorPrecio.TabIndex = 9;
            lblValorPrecio.Text = "-";
            // 
            // lblEtiqPrecio
            // 
            lblEtiqPrecio.AutoSize = true;
            lblEtiqPrecio.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqPrecio.Location = new Point(25, 192);
            lblEtiqPrecio.Name = "lblEtiqPrecio";
            lblEtiqPrecio.Size = new Size(111, 17);
            lblEtiqPrecio.TabIndex = 8;
            lblEtiqPrecio.Text = "Precio Acordado:";
            // 
            // lblValorCompradorId
            // 
            lblValorCompradorId.AutoSize = true;
            lblValorCompradorId.Font = new Font("Segoe UI", 9.5F);
            lblValorCompradorId.Location = new Point(140, 154);
            lblValorCompradorId.Name = "lblValorCompradorId";
            lblValorCompradorId.Size = new Size(13, 17);
            lblValorCompradorId.TabIndex = 7;
            lblValorCompradorId.Text = "-";
            // 
            // lblEtiqCompradorId
            // 
            lblEtiqCompradorId.AutoSize = true;
            lblEtiqCompradorId.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqCompradorId.Location = new Point(25, 154);
            lblEtiqCompradorId.Name = "lblEtiqCompradorId";
            lblEtiqCompradorId.Size = new Size(102, 17);
            lblEtiqCompradorId.TabIndex = 6;
            lblEtiqCompradorId.Text = "Comprador ID:";
            // 
            // lblValorVehiculoId
            // 
            lblValorVehiculoId.AutoSize = true;
            lblValorVehiculoId.Font = new Font("Segoe UI", 9.5F);
            lblValorVehiculoId.Location = new Point(140, 115);
            lblValorVehiculoId.Name = "lblValorVehiculoId";
            lblValorVehiculoId.Size = new Size(13, 17);
            lblValorVehiculoId.TabIndex = 5;
            lblValorVehiculoId.Text = "-";
            // 
            // lblEtiqVehiculoId
            // 
            lblEtiqVehiculoId.AutoSize = true;
            lblEtiqVehiculoId.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqVehiculoId.Location = new Point(25, 115);
            lblEtiqVehiculoId.Name = "lblEtiqVehiculoId";
            lblEtiqVehiculoId.Size = new Size(84, 17);
            lblEtiqVehiculoId.TabIndex = 4;
            lblEtiqVehiculoId.Text = "Vehículo ID:";
            // 
            // lblValorOperacionId
            // 
            lblValorOperacionId.AutoSize = true;
            lblValorOperacionId.Font = new Font("Segoe UI", 9.5F);
            lblValorOperacionId.Location = new Point(140, 77);
            lblValorOperacionId.Name = "lblValorOperacionId";
            lblValorOperacionId.Size = new Size(13, 17);
            lblValorOperacionId.TabIndex = 3;
            lblValorOperacionId.Text = "-";
            // 
            // lblEtiqOperacionId
            // 
            lblEtiqOperacionId.AutoSize = true;
            lblEtiqOperacionId.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqOperacionId.Location = new Point(25, 77);
            lblEtiqOperacionId.Name = "lblEtiqOperacionId";
            lblEtiqOperacionId.Size = new Size(95, 17);
            lblEtiqOperacionId.TabIndex = 2;
            lblEtiqOperacionId.Text = "Operación ID:";
            // 
            // lblValorVentaId
            // 
            lblValorVentaId.AutoSize = true;
            lblValorVentaId.Font = new Font("Segoe UI", 9.5F);
            lblValorVentaId.Location = new Point(140, 39);
            lblValorVentaId.Name = "lblValorVentaId";
            lblValorVentaId.Size = new Size(13, 17);
            lblValorVentaId.TabIndex = 1;
            lblValorVentaId.Text = "-";
            // 
            // lblEtiqVentaId
            // 
            lblEtiqVentaId.AutoSize = true;
            lblEtiqVentaId.Font = new Font("Segoe UI", 9.5F, FontStyle.Bold);
            lblEtiqVentaId.Location = new Point(25, 39);
            lblEtiqVentaId.Name = "lblEtiqVentaId";
            lblEtiqVentaId.Size = new Size(67, 17);
            lblEtiqVentaId.TabIndex = 0;
            lblEtiqVentaId.Text = "Venta ID:";
            // 
            // Form1
            // 
            AutoScaleDimensions = new SizeF(7F, 15F);
            AutoScaleMode = AutoScaleMode.Font;
            ClientSize = new Size(875, 570);
            Controls.Add(tabControlPrincipal);
            Controls.Add(txtToken);
            Controls.Add(lblToken);
            Controls.Add(lblSubtitulo);
            Controls.Add(lblTitulo);
            Name = "Form1";
            StartPosition = FormStartPosition.CenterScreen;
            Text = "Sistema de Consulta SOA - Vehículos y Ventas";
            tabControlPrincipal.ResumeLayout(false);
            tabVehiculos.ResumeLayout(false);
            tabVehiculos.PerformLayout();
            ((System.ComponentModel.ISupportInitialize)dgcVehiculos).EndInit();
            tabVentas.ResumeLayout(false);
            tabVentas.PerformLayout();
            grpDetalleVenta.ResumeLayout(false);
            grpDetalleVenta.PerformLayout();
            ResumeLayout(false);
            PerformLayout();
        }

        #endregion

        private Label lblTitulo;
        private Label lblSubtitulo;
        private Label lblToken;
        private TextBox txtToken;
        private TabControl tabControlPrincipal;
        private TabPage tabVehiculos;
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
        private TabPage tabVentas;
        private Label lblIdVenta;
        private TextBox txtIdVenta;
        private Button btnConsultarVenta;
        private Label lblEstadoVenta;
        private GroupBox grpDetalleVenta;
        private Label lblEtiqVentaId;
        private Label lblValorVentaId;
        private Label lblEtiqOperacionId;
        private Label lblValorOperacionId;
        private Label lblEtiqVehiculoId;
        private Label lblValorVehiculoId;
        private Label lblEtiqCompradorId;
        private Label lblValorCompradorId;
        private Label lblEtiqPrecio;
        private Label lblValorPrecio;
        private Label lblEtiqMoneda;
        private Label lblValorMoneda;
        private Label lblEtiqEstadoVenta;
        private Label lblValorEstadoVenta;
        private Label lblEtiqNotif;
        private Label lblValorNotificacion;
        private Label lblEtiqFecha;
        private Label lblValorFecha;
    }
}
