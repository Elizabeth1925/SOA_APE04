import {
  normalizarToken,
  loginOAuth,
  listarVehiculos,
  listarCompradores,
  registrarVenta,
  consultarVenta,
} from './api.js?v=2';

// Estado en memoria volátil de la aplicación (nunca persistido en disco)
let tokenActual = '';
let catalogoVehiculos = [];
let listaCompradores = [];

// Elementos del DOM - Login y Autenticación
const formLogin = document.querySelector('#formularioLogin');
const selectUsuario = document.querySelector('#usuario');
const inputContrasena = document.querySelector('#contrasena');
const btnLogin = document.querySelector('#botonLogin');
const formularioToken = document.querySelector('#formulario');
const campoToken = document.querySelector('#jwt');
const botonMostrar = document.querySelector('#alternarToken');
const botonBorrar = document.querySelector('#borrar');
const botonConsultarVehiculos = document.querySelector('#consultar');
const infoSesion = document.querySelector('#infoSesion');
const mensaje = document.querySelector('#mensaje');

// Elementos del DOM - Emisión de Venta
const formVenta = document.querySelector('#formularioVenta');
const selectVehiculo = document.querySelector('#selectVehiculo');
const selectComprador = document.querySelector('#selectComprador');
const inputPrecioVenta = document.querySelector('#precioVenta');
const inputIdempotencyKey = document.querySelector('#idempotencyKey');
const btnNuevoUuid = document.querySelector('#btnNuevoUuid');
const btnClaveMisma = document.querySelector('#btnClaveMisma');
const btnRegistrarVenta = document.querySelector('#btnRegistrarVenta');
const btnRecargarVehiculos = document.querySelector('#btnRecargarVehiculos');
const btnRecargarCompradores = document.querySelector('#btnRecargarCompradores');

const contenedorResultadoVenta = document.querySelector('#contenedorResultadoVenta');
const badgeEstadoVenta = document.querySelector('#badgeEstadoVenta');
const codigoResultadoVenta = document.querySelector('#codigoResultadoVenta');

// Elementos del DOM - Consulta de Venta por ID
const inputIdVentaBuscar = document.querySelector('#idVentaBuscar');
const btnBuscarVenta = document.querySelector('#btnBuscarVenta');
const contenedorDetalleVenta = document.querySelector('#contenedorDetalleVenta');
const codigoDetalleVenta = document.querySelector('#codigoDetalleVenta');

// Elementos del DOM - Listado de Vehículos
const contenedorVehiculos = document.querySelector('#vehiculos');
const contadorVehiculos = document.querySelector('#contador');
const conexion = document.querySelector('#conexion');

const formatoMoneda = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' });

function mostrarMensaje(texto, tipo = 'neutro') {
  mensaje.textContent = texto;
  mensaje.className = `mensaje ${tipo}`;
}

function generarUuid() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

function asignarNuevoUuid() {
  const nuevoUuid = generarUuid();
  inputIdempotencyKey.value = nuevoUuid;
  return nuevoUuid;
}

function texto(valor) {
  return valor === null || valor === undefined || valor === '' ? '—' : String(valor);
}

function precio(valor) {
  if (valor === null || valor === undefined || valor === '') return '—';
  const numero = Number(valor);
  return Number.isFinite(numero) ? formatoMoneda.format(numero) : texto(valor);
}

function limpiarListado() {
  contenedorVehiculos.replaceChildren();
  contadorVehiculos.textContent = '0 vehículos';
}

function crearElemento(etiqueta, clase, contenido) {
  const elemento = document.createElement(etiqueta);
  if (clase) elemento.className = clase;
  if (contenido !== undefined) elemento.textContent = String(contenido);
  return elemento;
}

function agregarDato(grilla, etiqueta, valor, extra = '') {
  const grupo = crearElemento('div', extra);
  grupo.append(
    crearElemento('span', 'dato-etiqueta', etiqueta),
    crearElemento('span', 'dato-valor', valor),
  );
  grilla.append(grupo);
}

function pintarListado(vehiculos) {
  limpiarListado();
  contadorVehiculos.textContent = `${vehiculos.length} vehículo${vehiculos.length === 1 ? '' : 's'}`;

  if (vehiculos.length === 0) {
    contenedorVehiculos.append(crearElemento('p', 'vacio', 'HTTP 200: no hay vehículos registrados.'));
    return;
  }

  for (const v of vehiculos) {
    const tarjeta = crearElemento('article', 'vehiculo');
    const cabecera = crearElemento('div', 'vehiculo-cabecera');
    const encabezado = crearElemento('div');
    const idVeh = v.idVehiculo ?? v.id;
    encabezado.append(
      crearElemento('h3', 'vehiculo-titulo', `${texto(v.marca)} ${texto(v.modelo)}`),
      crearElemento('span', 'vehiculo-id', `ID: ${texto(idVeh)}`),
    );

    const estado = texto(v.estado);
    const claseEstado = String(estado).toUpperCase() === 'DISPONIBLE' ? 'disponible'
      : String(estado).toUpperCase() === 'VENDIDO' ? 'vendido' : '';
    cabecera.append(encabezado, crearElemento('span', `estado ${claseEstado}`, estado));

    const grilla = crearElemento('div', 'datos');
    agregarDato(grilla, 'Placa', texto(v.placa));
    agregarDato(grilla, 'Año', texto(v.anio));
    agregarDato(grilla, 'Color', texto(v.color));
    agregarDato(grilla, 'Precio', precio(v.precio), 'dato-precio');
    tarjeta.append(cabecera, grilla);
    contenedorVehiculos.append(tarjeta);
  }
}

async function cargarVehiculos() {
  try {
    const res = await listarVehiculos(tokenActual);
    catalogoVehiculos = res.vehiculos;
    pintarListado(catalogoVehiculos);

    // Actualizar selector de venta
    selectVehiculo.replaceChildren();
    const opcionVacia = document.createElement('option');
    opcionVacia.value = '';
    opcionVacia.textContent = '-- Seleccione un vehículo disponible --';
    selectVehiculo.append(opcionVacia);

    for (const v of catalogoVehiculos) {
      const id = v.idVehiculo ?? v.id;
      const opt = document.createElement('option');
      opt.value = id;
      const est = (v.estado || '').toUpperCase();
      opt.textContent = `[ID ${id}] ${v.marca} ${v.modelo} (${v.placa}) - ${precio(v.precio)} [${est}]`;
      if (est === 'VENDIDO') {
        opt.disabled = true;
        opt.textContent += ' (No disponible)';
      }
      selectVehiculo.append(opt);
    }
  } catch (error) {
    mostrarMensaje(error.message, 'error');
  }
}

async function cargarCompradores() {
  try {
    const res = await listarCompradores(tokenActual);
    listaCompradores = res.compradores;

    selectComprador.replaceChildren();
    const opcionVacia = document.createElement('option');
    opcionVacia.value = '';
    opcionVacia.textContent = listaCompradores.length > 0
      ? `-- Seleccione un comprador (${listaCompradores.length} disponibles) --`
      : '-- No hay compradores registrados --';
    selectComprador.append(opcionVacia);

    for (const c of listaCompradores) {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `[ID ${c.id}] ${c.nombre} (${c.documento || c.cedula || c.identificacion || 'S/D'})`;
      selectComprador.append(opt);
    }
    return true;
  } catch (error) {
    selectComprador.replaceChildren();
    const optErr = document.createElement('option');
    optErr.value = '';
    optErr.textContent = `-- Error al cargar compradores (${error.codigoHttp ? 'HTTP ' + error.codigoHttp : 'Sin conexión'}) --`;
    selectComprador.append(optErr);
    mostrarMensaje(`Compradores: ${error.message}`, 'error');
    return false;
  }
}

// Limpiar contraseña al cambiar de usuario en el selector
selectUsuario.addEventListener('change', () => {
  inputContrasena.value = '';
  inputContrasena.focus();
});

// Evento cambio de vehículo para autocompletar precio
selectVehiculo.addEventListener('change', () => {
  const idSeleccionado = Number(selectVehiculo.value);
  const vehiculoEncontrado = catalogoVehiculos.find((v) => (v.idVehiculo ?? v.id) === idSeleccionado);
  if (vehiculoEncontrado && vehiculoEncontrado.precio) {
    inputPrecioVenta.value = vehiculoEncontrado.precio;
  }
});

// Evento de Login
formLogin.addEventListener('submit', async (e) => {
  e.preventDefault();
  const usuario = selectUsuario.value;
  const contrasena = inputContrasena.value;

  if (!contrasena.trim()) {
    mostrarMensaje('Ingrese la contraseña de la cuenta para iniciar sesión.', 'error');
    inputContrasena.focus();
    return;
  }

  btnLogin.disabled = true;
  mostrarMensaje('Autenticando contra Spring Boot vía NGINX…');

  try {
    const res = await loginOAuth(usuario, contrasena);
    tokenActual = res.token;
    campoToken.value = tokenActual;
    campoToken.type = 'password';
    botonMostrar.textContent = 'Mostrar';
    botonMostrar.setAttribute('aria-pressed', 'false');
    infoSesion.textContent = `Sesión activa: ${usuario} | Scope: ${res.scope || 'vehiculos'} | Expiración: ${res.expiraEn}s (JWT protegido en memoria)`;
    mostrarMensaje(`HTTP ${res.codigoHttp}: Autenticación exitosa como "${usuario}". Cargando catálogo y compradores…`, 'exito');

    // Cargar listas operativas
    await cargarVehiculos();
    const compradoresOk = await cargarCompradores();
    asignarNuevoUuid();
    if (compradoresOk) {
      mostrarMensaje(`HTTP ${res.codigoHttp}: Sesión activa como "${usuario}". ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
    }
  } catch (error) {
    infoSesion.textContent = 'Estado: Error de autenticación';
    mostrarMensaje(error.message, 'error');
  } finally {
    btnLogin.disabled = false;
  }
});

// Actualización manual de token
formularioToken.addEventListener('submit', async (e) => {
  e.preventDefault();
  tokenActual = normalizarToken(campoToken.value);
  if (!tokenActual) {
    mostrarMensaje('Por favor ingrese un token JWT válido o inicie sesión.', 'error');
    return;
  }
  infoSesion.textContent = 'Sesión manual establecida con JWT provisto en memoria.';
  mostrarMensaje('Consultando catálogo y compradores con token manual…');
  await cargarVehiculos();
  const compradoresOk = await cargarCompradores();
  if (compradoresOk && catalogoVehiculos.length > 0) {
    mostrarMensaje(`HTTP 200: ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
  }
});

botonMostrar.addEventListener('click', () => {
  const revelar = campoToken.type === 'password';
  campoToken.type = revelar ? 'text' : 'password';
  botonMostrar.textContent = revelar ? 'Ocultar' : 'Mostrar';
  botonMostrar.setAttribute('aria-pressed', String(revelar));
});

botonBorrar.addEventListener('click', () => {
  tokenActual = '';
  campoToken.value = '';
  campoToken.type = 'password';
  botonMostrar.textContent = 'Mostrar';
  botonMostrar.setAttribute('aria-pressed', 'false');
  infoSesion.textContent = 'Estado: No autenticado';
  limpiarListado();
  selectVehiculo.innerHTML = '<option value="">-- Inicie sesión para cargar vehículos --</option>';
  selectComprador.innerHTML = '<option value="">-- Inicie sesión para cargar compradores --</option>';
  contenedorResultadoVenta.style.display = 'none';
  contenedorDetalleVenta.style.display = 'none';
  mostrarMensaje('Sesión y token en memoria volátil eliminados.');
});

// Botones de Idempotency-Key
btnNuevoUuid.addEventListener('click', () => {
  const nuevo = asignarNuevoUuid();
  mostrarMensaje(`Nueva clave de idempotencia generada: ${nuevo}`);
});

btnClaveMisma.addEventListener('click', () => {
  mostrarMensaje(`Clave de idempotencia actual retenida para prueba de reintento: ${inputIdempotencyKey.value}`);
});

btnRecargarVehiculos.addEventListener('click', () => cargarVehiculos());
btnRecargarCompradores.addEventListener('click', () => cargarCompradores());

// Registro de Venta
formVenta.addEventListener('submit', async (e) => {
  e.preventDefault();
  const vehiculoId = Number(selectVehiculo.value);
  const compradorId = Number(selectComprador.value);
  const precioVenta = parseFloat(inputPrecioVenta.value);
  const idempotencyKey = inputIdempotencyKey.value.trim();

  if (!vehiculoId) {
    mostrarMensaje('Seleccione un vehículo disponible para la venta.', 'error');
    return;
  }
  if (!compradorId) {
    mostrarMensaje('Seleccione un comprador para la venta.', 'error');
    return;
  }
  if (!precioVenta || precioVenta <= 0) {
    mostrarMensaje('Ingrese un precio de venta válido mayor a cero.', 'error');
    return;
  }
  if (!idempotencyKey) {
    mostrarMensaje('La clave Idempotency-Key es obligatoria.', 'error');
    return;
  }

  btnRegistrarVenta.disabled = true;
  mostrarMensaje('Enviando transacción de venta a través de NGINX (POST /api/ventas)...');

  try {
    const payload = {
      vehiculoId: vehiculoId,
      compradorId: compradorId,
      precio: precioVenta,
    };

    const res = await registrarVenta(payload, idempotencyKey, tokenActual);

    contenedorResultadoVenta.style.display = 'block';
    badgeEstadoVenta.className = 'badge-http ok';
    badgeEstadoVenta.textContent = `HTTP ${res.codigoHttp}`;
    codigoResultadoVenta.textContent = JSON.stringify(res.venta, null, 2);

    if (res.venta?.id) {
      inputIdVentaBuscar.value = res.venta.id;
    }

    const estadoTxt = res.venta?.estado || 'PROCESADA';
    const notifTxt = res.venta?.notificacion || 'PENDIENTE';
    mostrarMensaje(
      `HTTP ${res.codigoHttp}: Venta ID ${res.venta?.id ?? '—'} [${estadoTxt}] | Notificación RabbitMQ: ${notifTxt}.`,
      'exito',
    );

    // Refrescar catálogo para observar cambio de estado del vehículo
    await cargarVehiculos();
  } catch (error) {
    contenedorResultadoVenta.style.display = 'block';
    const codigo = error.codigoHttp || 400;
    badgeEstadoVenta.className = codigo === 409 ? 'badge-http conflict' : 'badge-http error';
    badgeEstadoVenta.textContent = `HTTP ${codigo}`;
    codigoResultadoVenta.textContent = JSON.stringify(error.detalles || { error: error.message }, null, 2);

    mostrarMensaje(error.message, 'error');
  } finally {
    btnRegistrarVenta.disabled = false;
  }
});

// Consulta de Venta por ID
btnBuscarVenta.addEventListener('click', async () => {
  const idVenta = inputIdVentaBuscar.value.trim();
  if (!idVenta) {
    mostrarMensaje('Ingrese un ID de venta válido para consultar.', 'error');
    return;
  }

  btnBuscarVenta.disabled = true;
  try {
    const res = await consultarVenta(idVenta, tokenActual);
    contenedorDetalleVenta.style.display = 'block';
    codigoDetalleVenta.textContent = JSON.stringify(res.venta, null, 2);
    mostrarMensaje(`Consulta de venta ID ${idVenta} completada exitosamente.`, 'exito');
  } catch (error) {
    contenedorDetalleVenta.style.display = 'block';
    codigoDetalleVenta.textContent = JSON.stringify({ error: error.message }, null, 2);
    mostrarMensaje(error.message, 'error');
  } finally {
    btnBuscarVenta.disabled = false;
  }
});

function actualizarConexion() {
  conexion.textContent = navigator.onLine ? 'Navegador conectado' : 'Sin conexión de red';
}
window.addEventListener('online', actualizarConexion);
window.addEventListener('offline', actualizarConexion);
actualizarConexion();

// Inicializar UUID inicial
asignarNuevoUuid();

// Registro de Service Worker para capacidades PWA
if ('serviceWorker' in navigator && window.isSecureContext) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}
