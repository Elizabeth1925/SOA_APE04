using ConsultaVehiculosForms.Models;
using ConsultaVehiculosForms.Services;

namespace ConsultaVehiculosForms
{
    public partial class Form1 : Form
    {
        private readonly VehiculoApiClient _vehiculoClient = new();
        private readonly VentaApiClient _ventaClient = new();

        public Form1()
        {
            InitializeComponent();
        }

        // ==========================================
        // 1. CATÁLOGO DE VEHÍCULOS (Java :8080)
        // ==========================================

        /// <summary>
        /// Requerimiento 1 y 2 (Pág. 19): Listar en DataGridView y filtrar por marca (?marca=...).
        /// </summary>
        private async void btnConsultar_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string marca = txtFiltroMarca.Text.Trim();

            btnConsultar.Enabled = false;
            btnBuscarId.Enabled = false;
            lblEstado.Text = "Estado: Consultando catálogo en Java (:8080)...";
            lblEstado.ForeColor = Color.Blue;

            try
            {
                var response = await _vehiculoClient.ListarVehiculosAsync(
                    token,
                    string.IsNullOrWhiteSpace(marca) ? null : marca
                );

                if (response.IsSuccess && response.Data != null)
                {
                    dgcVehiculos.DataSource = response.Data;
                    lblEstado.Text = string.IsNullOrWhiteSpace(marca)
                        ? "Estado: Catálogo completo cargado (HTTP 200 OK)"
                        : $"Estado: Filtro por marca '{marca}' aplicado (HTTP 200 OK)";
                    lblEstado.ForeColor = Color.DarkGreen;
                    lblTotal.Text = $"Total: {response.Data.Count} vehículos encontrados";
                }
                else
                {
                    dgcVehiculos.DataSource = null;
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    lblEstado.ForeColor = Color.Crimson;
                    lblTotal.Text = "Total: 0 vehículos";

                    MessageBox.Show(
                        response.ErrorMessage ?? "Error al consultar el catálogo de vehículos.",
                        "Respuesta de la API",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Warning
                    );
                }
            }
            catch (Exception ex)
            {
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                lblEstado.ForeColor = Color.Crimson;
            }
            finally
            {
                btnConsultar.Enabled = true;
                btnBuscarId.Enabled = true;
            }
        }

        /// <summary>
        /// Requerimiento 3 (Pág. 19): Consultar el detalle de un vehículo por su ID (/api/vehiculos/{id}).
        /// </summary>
        private async void btnBuscarId_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string idTexto = txtIdVehiculo.Text.Trim();

            if (!int.TryParse(idTexto, out int id) || id <= 0)
            {
                MessageBox.Show(
                    "Por favor ingrese un ID numérico válido mayor a 0.",
                    "Validación de Entrada",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information
                );
                txtIdVehiculo.Focus();
                return;
            }

            btnConsultar.Enabled = false;
            btnBuscarId.Enabled = false;
            lblEstado.Text = $"Estado: Buscando vehículo con ID {id} en Java (:8080)...";
            lblEstado.ForeColor = Color.Blue;

            try
            {
                var response = await _vehiculoClient.ObtenerVehiculoPorIdAsync(token, id);

                if (response.IsSuccess && response.Data != null)
                {
                    // Mostrar el vehículo consultado en la grilla
                    dgcVehiculos.DataSource = new List<VehiculoDto> { response.Data };
                    lblEstado.Text = $"Estado: Vehículo con ID {id} encontrado con éxito (HTTP 200 OK)";
                    lblEstado.ForeColor = Color.DarkGreen;
                    lblTotal.Text = "Total: 1 vehículo";
                }
                else
                {
                    dgcVehiculos.DataSource = null;
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    lblEstado.ForeColor = Color.Crimson;
                    lblTotal.Text = "Total: 0 vehículos";

                    MessageBox.Show(
                        response.ErrorMessage ?? $"No se encontró el vehículo con ID {id}.",
                        "Detalle de Vehículo",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Warning
                    );
                }
            }
            catch (Exception ex)
            {
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                lblEstado.ForeColor = Color.Crimson;
            }
            finally
            {
                btnConsultar.Enabled = true;
                btnBuscarId.Enabled = true;
            }
        }

        private void btnLimpiarVehiculos_Click(object sender, EventArgs e)
        {
            txtFiltroMarca.Clear();
            txtIdVehiculo.Clear();
            dgcVehiculos.DataSource = null;
            lblEstado.Text = "Estado: En espera de consulta";
            lblEstado.ForeColor = Color.DimGray;
            lblTotal.Text = "Total: 0 vehículos";
        }

        // ==========================================
        // 2. VISTA DE VENTA (Python :8002)
        // ==========================================

        /// <summary>
        /// Requerimiento 5 (Pág. 19): Mostrar una vista de venta con estado y notificación (GET /api/ventas/{id}).
        /// </summary>
        private async void btnConsultarVenta_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string idTexto = txtIdVenta.Text.Trim();

            if (!int.TryParse(idTexto, out int idVenta) || idVenta <= 0)
            {
                MessageBox.Show(
                    "Por favor ingrese un ID de venta numérico válido mayor a 0.",
                    "Validación de Entrada",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Information
                );
                txtIdVenta.Focus();
                return;
            }

            btnConsultarVenta.Enabled = false;
            lblEstadoVenta.Text = $"Estado: Consultando venta con ID {idVenta} en Python (:8002)...";
            lblEstadoVenta.ForeColor = Color.Blue;

            try
            {
                var response = await _ventaClient.ObtenerVentaPorIdAsync(token, idVenta);

                if (response.IsSuccess && response.Data != null)
                {
                    var v = response.Data;
                    lblValorVentaId.Text = v.Id.ToString();
                    lblValorOperacionId.Text = v.OperacionId ?? "-";
                    lblValorVehiculoId.Text = v.VehiculoId.ToString();
                    lblValorCompradorId.Text = v.CompradorId.ToString();
                    lblValorPrecio.Text = v.PrecioAcordado ?? "-";
                    lblValorMoneda.Text = v.Moneda ?? "-";
                    lblValorFecha.Text = v.FechaUTC ?? "-";

                    // Resalta el estado según las especificaciones del PDF (diapositivas 13 y 34)
                    lblValorEstadoVenta.Text = v.Estado ?? "-";
                    lblValorEstadoVenta.ForeColor = (v.Estado?.ToUpperInvariant()) switch
                    {
                        "COMPLETADA" => Color.DarkGreen,
                        "EN_REVISION" => Color.DarkOrange,
                        "FALLIDA" => Color.Red,
                        _ => Color.Black
                    };

                    // Resalta la notificación (ENVIADA o PENDIENTE)
                    lblValorNotificacion.Text = v.Notificacion ?? "-";
                    lblValorNotificacion.ForeColor = (v.Notificacion?.ToUpperInvariant()) switch
                    {
                        "ENVIADA" => Color.DarkGreen,
                        "PENDIENTE" => Color.DarkOrange,
                        _ => Color.DarkSlateGray
                    };

                    lblEstadoVenta.Text = "Estado: Venta consultada con éxito (HTTP 200 OK)";
                    lblEstadoVenta.ForeColor = Color.DarkGreen;
                }
                else
                {
                    LimpiarDetalleVenta();
                    lblEstadoVenta.Text = $"Estado: {response.ErrorMessage}";
                    lblEstadoVenta.ForeColor = Color.Crimson;

                    MessageBox.Show(
                        response.ErrorMessage ?? "Error al consultar la venta.",
                        "Consulta de Venta",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Warning
                    );
                }
            }
            catch (Exception ex)
            {
                lblEstadoVenta.Text = $"Estado: Error - {ex.Message}";
                lblEstadoVenta.ForeColor = Color.Crimson;
            }
            finally
            {
                btnConsultarVenta.Enabled = true;
            }
        }

        private void LimpiarDetalleVenta()
        {
            lblValorVentaId.Text = "-";
            lblValorOperacionId.Text = "-";
            lblValorVehiculoId.Text = "-";
            lblValorCompradorId.Text = "-";
            lblValorPrecio.Text = "-";
            lblValorMoneda.Text = "-";
            lblValorFecha.Text = "-";
            lblValorEstadoVenta.Text = "-";
            lblValorEstadoVenta.ForeColor = Color.Black;
            lblValorNotificacion.Text = "-";
            lblValorNotificacion.ForeColor = Color.DarkSlateGray;
        }
    }
}
