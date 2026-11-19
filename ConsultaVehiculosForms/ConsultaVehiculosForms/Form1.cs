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
        // 1. VEHÍCULOS (Java Spring Boot vía NGINX :8090)
        // ==========================================

        /// <summary>
        /// Operación 1: Dar de alta un vehículo (POST /api/vehiculos).
        /// </summary>
        private async void btnCrearVehiculo_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string marca = txtVMarca.Text.Trim();
            string modelo = txtVModelo.Text.Trim();
            string placa = txtVPlaca.Text.Trim();
            string chasis = txtVChasis.Text.Trim();
            string anioTxt = txtVAnio.Text.Trim();
            string color = txtVColor.Text.Trim();
            string precioTxt = txtVPrecio.Text.Trim();

            if (string.IsNullOrWhiteSpace(marca) || string.IsNullOrWhiteSpace(modelo) ||
                string.IsNullOrWhiteSpace(placa) || string.IsNullOrWhiteSpace(chasis))
            {
                MessageBox.Show("Por favor complete los campos obligatorios: Marca, Modelo, Placa y Chasis.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            if (chasis.Length != 17)
            {
                MessageBox.Show("El chasis debe contener exactamente 17 caracteres según el estándar de la API.",
                    "Validación de Chasis", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                txtVChasis.Focus();
                return;
            }

            if (!int.TryParse(anioTxt, out int anio) || anio < 1900 || anio > 2030)
            {
                MessageBox.Show("Por favor ingrese un año válido de 4 dígitos (ej: 2024).",
                    "Validación de Año", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                txtVAnio.Focus();
                return;
            }

            if (!decimal.TryParse(precioTxt, out decimal precio) || precio <= 0)
            {
                MessageBox.Show("Por favor ingrese un precio numérico mayor a cero.",
                    "Validación de Precio", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                txtVPrecio.Focus();
                return;
            }

            btnCrearVehiculo.Enabled = false;
            lblEstado.Text = "Estado: Registrando vehículo en Java a través de NGINX (:8090)...";
            lblEstado.ForeColor = Color.Blue;

            try
            {
                var request = new CrearVehiculoRequest
                {
                    Marca = marca,
                    Modelo = modelo,
                    Placa = placa,
                    Chasis = chasis,
                    Anio = anio,
                    Color = color,
                    Precio = precio
                };

                var response = await _vehiculoClient.CrearVehiculoAsync(token, request);

                if (response.IsSuccess && response.Data != null)
                {
                    lblEstado.Text = $"Estado: Vehículo registrado exitosamente con ID {response.Data.IdVehiculo} (HTTP 201 Created)";
                    lblEstado.ForeColor = Color.DarkGreen;

                    MessageBox.Show(
                        $"Vehículo registrado con éxito!\nID generado: {response.Data.IdVehiculo}\nMarca: {response.Data.Marca}\nModelo: {response.Data.Modelo}\nCódigo HTTP: 201 Created",
                        "Creación Exitosa", MessageBoxButtons.OK, MessageBoxIcon.Information);

                    // Limpiar formulario de alta
                    txtVPlaca.Clear();
                    txtVChasis.Clear();
                    txtVPrecio.Clear();

                    // Refrescar listado
                    btnConsultar_Click(sender, e);
                }
                else
                {
                    lblEstado.Text = $"Estado: {response.ErrorMessage}";
                    lblEstado.ForeColor = Color.Crimson;

                    MessageBox.Show(response.ErrorMessage ?? "Error al registrar vehículo.",
                        "Error HTTP", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            catch (Exception ex)
            {
                lblEstado.Text = $"Estado: Error - {ex.Message}";
                lblEstado.ForeColor = Color.Crimson;
            }
            finally
            {
                btnCrearVehiculo.Enabled = true;
            }
        }

        /// <summary>
        /// Operación 2: Listar en DataGridView y filtrar por marca (GET /api/vehiculos).
        /// </summary>
        private async void btnConsultar_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string marca = txtFiltroMarca.Text.Trim();

            btnConsultar.Enabled = false;
            btnBuscarId.Enabled = false;
            lblEstado.Text = "Estado: Consultando catálogo de vehículos en NGINX (:8090)...";
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
                        ? $"Estado: Catálogo completo cargado con éxito (HTTP {(int)response.StatusCode} OK)"
                        : $"Estado: Filtro por marca '{marca}' aplicado (HTTP {(int)response.StatusCode} OK)";
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
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
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
        /// Consulta el detalle de un vehículo por su ID (GET /api/vehiculos/{id}).
        /// </summary>
        private async void btnBuscarId_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string idTexto = txtIdVehiculo.Text.Trim();

            if (!int.TryParse(idTexto, out int id) || id <= 0)
            {
                MessageBox.Show("Por favor ingrese un ID numérico válido mayor a 0.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Information);
                txtIdVehiculo.Focus();
                return;
            }

            btnConsultar.Enabled = false;
            btnBuscarId.Enabled = false;
            lblEstado.Text = $"Estado: Buscando vehículo con ID {id} en NGINX (:8090)...";
            lblEstado.ForeColor = Color.Blue;

            try
            {
                var response = await _vehiculoClient.ObtenerVehiculoPorIdAsync(token, id);

                if (response.IsSuccess && response.Data != null)
                {
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

                    MessageBox.Show(response.ErrorMessage ?? $"No se encontró el vehículo con ID {id}.",
                        "Detalle de Vehículo", MessageBoxButtons.OK, MessageBoxIcon.Warning);
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
        // 2. COMPRADORES (Python FastAPI vía NGINX :8090)
        // ==========================================

        /// <summary>
        /// Operación 3: Registrar comprador (POST /api/compradores).
        /// </summary>
        private async void btnCrearComprador_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string doc = txtCDocumento.Text.Trim();
            string nom = txtCNombre.Text.Trim();
            string cor = txtCCorreo.Text.Trim();

            if (string.IsNullOrWhiteSpace(doc) || string.IsNullOrWhiteSpace(nom) || string.IsNullOrWhiteSpace(cor))
            {
                MessageBox.Show("Por favor ingrese todos los datos: Documento, Nombre y Correo electrónico.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            btnCrearComprador.Enabled = false;
            lblEstadoCompradores.Text = "Estado: Registrando comprador en FastAPI (:8090)...";
            lblEstadoCompradores.ForeColor = Color.Blue;

            try
            {
                var request = new CrearCompradorRequest
                {
                    Documento = doc,
                    Nombre = nom,
                    Correo = cor
                };

                var response = await _ventaClient.CrearCompradorAsync(token, request);

                if (response.IsSuccess && response.Data != null)
                {
                    lblEstadoCompradores.Text = $"Estado: Comprador registrado exitosamente con ID {response.Data.Id} (HTTP 201 Created)";
                    lblEstadoCompradores.ForeColor = Color.DarkGreen;

                    MessageBox.Show(
                        $"Comprador registrado con éxito!\nID generado: {response.Data.Id}\nNombre: {response.Data.Nombre}\nDocumento: {response.Data.Documento}\nCódigo HTTP: 201 Created",
                        "Registro Exitoso", MessageBoxButtons.OK, MessageBoxIcon.Information);

                    txtCDocumento.Clear();
                    txtCNombre.Clear();
                    txtCCorreo.Clear();

                    btnListarCompradores_Click(sender, e);
                }
                else
                {
                    lblEstadoCompradores.Text = $"Estado: {response.ErrorMessage}";
                    lblEstadoCompradores.ForeColor = Color.Crimson;

                    MessageBox.Show(response.ErrorMessage ?? "Error al registrar comprador.",
                        "Error HTTP", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            catch (Exception ex)
            {
                lblEstadoCompradores.Text = $"Estado: Error - {ex.Message}";
                lblEstadoCompradores.ForeColor = Color.Crimson;
            }
            finally
            {
                btnCrearComprador.Enabled = true;
            }
        }

        /// <summary>
        /// Operación 4: Listar compradores autorizados (GET /api/compradores).
        /// </summary>
        private async void btnListarCompradores_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();

            btnListarCompradores.Enabled = false;
            lblEstadoCompradores.Text = "Estado: Consultando listado de compradores en NGINX (:8090)...";
            lblEstadoCompradores.ForeColor = Color.Blue;

            try
            {
                var response = await _ventaClient.ListarCompradoresAsync(token);

                if (response.IsSuccess && response.Data != null)
                {
                    dgcCompradores.DataSource = response.Data;
                    lblEstadoCompradores.Text = $"Estado: {response.Data.Count} compradores cargados con éxito (HTTP 200 OK)";
                    lblEstadoCompradores.ForeColor = Color.DarkGreen;
                    lblTotalCompradores.Text = $"Total: {response.Data.Count} compradores";
                }
                else
                {
                    dgcCompradores.DataSource = null;
                    lblEstadoCompradores.Text = $"Estado: {response.ErrorMessage}";
                    lblEstadoCompradores.ForeColor = Color.Crimson;
                    lblTotalCompradores.Text = "Total: 0 compradores";

                    MessageBox.Show(response.ErrorMessage ?? "Error al listar compradores.",
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            catch (Exception ex)
            {
                lblEstadoCompradores.Text = $"Estado: Error - {ex.Message}";
                lblEstadoCompradores.ForeColor = Color.Crimson;
            }
            finally
            {
                btnListarCompradores.Enabled = true;
            }
        }

        // ==========================================
        // 3. VENTAS (Python FastAPI vía NGINX :8090 - Consulta Exclusiva)
        // ==========================================

        /// <summary>
        /// Operación 5A: Listar histórico general de ventas (GET /api/ventas).
        /// </summary>
        private async void btnListarVentas_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();

            btnListarVentas.Enabled = false;
            lblEstadoVenta.Text = "Estado: Consultando listado de ventas en FastAPI (:8090)...";
            lblEstadoVenta.ForeColor = Color.Blue;

            try
            {
                var response = await _ventaClient.ListarVentasAsync(token);

                if (response.IsSuccess && response.Data != null)
                {
                    dgcVentas.DataSource = response.Data;
                    lblEstadoVenta.Text = $"Estado: {response.Data.Count} ventas consultadas con éxito (HTTP 200 OK)";
                    lblEstadoVenta.ForeColor = Color.DarkGreen;
                }
                else
                {
                    dgcVentas.DataSource = null;
                    lblEstadoVenta.Text = $"Estado: {response.ErrorMessage}";
                    lblEstadoVenta.ForeColor = Color.Crimson;

                    MessageBox.Show(response.ErrorMessage ?? "Error al consultar ventas.",
                        "Respuesta de la API", MessageBoxButtons.OK, MessageBoxIcon.Warning);
                }
            }
            catch (Exception ex)
            {
                lblEstadoVenta.Text = $"Estado: Error - {ex.Message}";
                lblEstadoVenta.ForeColor = Color.Crimson;
            }
            finally
            {
                btnListarVentas.Enabled = true;
            }
        }

        /// <summary>
        /// Operación 5B: Consultar una venta individual por su ID (GET /api/ventas/{id}).
        /// </summary>
        private async void btnConsultarVenta_Click(object sender, EventArgs e)
        {
            string token = txtToken.Text.Trim();
            string idTexto = txtIdVenta.Text.Trim();

            if (!int.TryParse(idTexto, out int idVenta) || idVenta <= 0)
            {
                MessageBox.Show("Por favor ingrese un ID de venta numérico válido mayor a 0.",
                    "Validación de Entrada", MessageBoxButtons.OK, MessageBoxIcon.Information);
                txtIdVenta.Focus();
                return;
            }

            btnConsultarVenta.Enabled = false;
            lblEstadoVenta.Text = $"Estado: Consultando venta con ID {idVenta} en NGINX (:8090)...";
            lblEstadoVenta.ForeColor = Color.Blue;

            try
            {
                var response = await _ventaClient.ObtenerVentaPorIdAsync(token, idVenta);

                if (response.IsSuccess && response.Data != null)
                {
                    MostrarDetalleVenta(response.Data);
                    lblEstadoVenta.Text = "Estado: Venta consultada con éxito (HTTP 200 OK)";
                    lblEstadoVenta.ForeColor = Color.DarkGreen;
                }
                else
                {
                    LimpiarDetalleVenta();
                    lblEstadoVenta.Text = $"Estado: {response.ErrorMessage}";
                    lblEstadoVenta.ForeColor = Color.Crimson;

                    MessageBox.Show(response.ErrorMessage ?? "Error al consultar la venta.",
                        "Consulta de Venta", MessageBoxButtons.OK, MessageBoxIcon.Warning);
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

        private void dgcVentas_SelectionChanged(object sender, EventArgs e)
        {
            if (dgcVentas.CurrentRow?.DataBoundItem is VentaDto seleccionada)
            {
                MostrarDetalleVenta(seleccionada);
            }
        }

        private void MostrarDetalleVenta(VentaDto v)
        {
            lblValorVentaId.Text = v.Id.ToString();
            lblValorOperacionId.Text = v.OperacionId ?? "-";
            lblValorVehiculoId.Text = v.VehiculoId.ToString();
            lblValorCompradorId.Text = v.CompradorId.ToString();
            lblValorPrecio.Text = v.PrecioAcordado ?? "-";
            lblValorMoneda.Text = v.Moneda ?? "-";
            lblValorFecha.Text = v.FechaUTC ?? "-";

            lblValorEstadoVenta.Text = v.Estado ?? "-";
            lblValorEstadoVenta.ForeColor = (v.Estado?.ToUpperInvariant()) switch
            {
                "COMPLETADA" => Color.DarkGreen,
                "EN_REVISION" => Color.DarkOrange,
                "FALLIDA" => Color.Red,
                _ => Color.Black
            };

            lblValorNotificacion.Text = v.Notificacion ?? "-";
            lblValorNotificacion.ForeColor = (v.Notificacion?.ToUpperInvariant()) switch
            {
                "ENVIADA" => Color.DarkGreen,
                "PENDIENTE" => Color.DarkOrange,
                _ => Color.DarkSlateGray
            };
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
