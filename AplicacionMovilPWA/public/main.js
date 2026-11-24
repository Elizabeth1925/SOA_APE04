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
let usuarioActual = '';
let catalogoVehiculos = [];
let listaCompradores = [];
let filtroEstadoActual = 'TODOS';

// Pantallas y navegación visual (Login separado -> App con pestañas)
const pantallaLogin = document.querySelector('#pantallaLogin');
const pantallaApp = document.querySelector('#pantallaApp');
const tabsMenu = document.querySelectorAll('.tab-link');
const vistasPanel = document.querySelectorAll('.vista-panel');
const nombreUsuarioChip = document.querySelector('#nombreUsuarioChip');
const btnCerrarSesionTop = document.querySelector('#btnCerrarSesionTop');
const btnIrAVentas = document.querySelector('#btnIrAVentas');
const btnIrAAutos = document.querySelector('#btnIrAAutos');
const btnRefrescarCatalogo = document.querySelector('#btnRefrescarCatalogo');
const botonesFiltro = document.querySelectorAll('.chip-filtro');

const statDisponibles = document.querySelector('#statDisponibles');
const statVendidos = document.querySelector('#statVendidos');
const statCompradores = document.querySelector('#statCompradores');

// Elementos del DOM - Login y Autenticación
const formLogin = document.querySelector('#formularioLogin');
const selectUsuario = document.querySelector('#usuario');
const inputContrasena = document.querySelector('#contrasena');
const btnLogin = document.querySelector('#botonLogin');
const mensajeLogin = document.querySelector('#mensajeLogin');
const formLoginManual = document.querySelector('#formularioLoginManual');
const inputJwtLoginManual = document.querySelector('#jwtLoginManual');

const formularioToken = document.querySelector('#formulario');
const campoToken = document.querySelector('#jwt');
const botonMostrar = document.querySelector('#alternarToken');
const botonBorrar = document.querySelector('#borrar');
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
const resumenVisualVenta = document.querySelector('#resumenVisualVenta');
const codigoResultadoVenta = document.querySelector('#codigoResultadoVenta');

// Elementos del DOM - Consulta de Venta por ID
const inputIdVentaBuscar = document.querySelector('#idVentaBuscar');
const btnBuscarVenta = document.querySelector('#btnBuscarVenta');
const contenedorDetalleVenta = document.querySelector('#contenedorDetalleVenta');
const resumenVisualDetalle = document.querySelector('#resumenVisualDetalle');
const codigoDetalleVenta = document.querySelector('#codigoDetalleVenta');

// Elementos del DOM - Listado de Vehículos
const contenedorVehiculos = document.querySelector('#vehiculos');
const contadorVehiculos = document.querySelector('#contador');
const conexion = document.querySelector('#conexion');

const formatoMoneda = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' });

function mostrarMensaje(texto, tipo = 'neutro') {
  mensaje.textContent = texto;
  mensaje.className = `mensaje ${tipo}`;
  if (!pantallaLogin.hidden) {
    mensajeLogin.style.display = 'block';
    mensajeLogin.textContent = texto;
    mensajeLogin.className = `mensaje ${tipo}`;
  }
}

function mostrarVista(nombre) {
  for (const tab of tabsMenu) {
    tab.classList.toggle('activa', tab.dataset.vista === nombre);
  }
  for (const panel of vistasPanel) {
    const activa = panel.id === `vista-${nombre}`;
    panel.hidden = !activa;
    panel.classList.toggle('activa', activa);
  }
}

function entrarEnApp(nombreUsuario) {
  usuarioActual = nombreUsuario || 'sesión JWT';
  nombreUsuarioChip.textContent = usuarioActual;
  pantallaLogin.hidden = true;
  pantallaApp.hidden = false;
  mensajeLogin.style.display = 'none';
  mostrarVista('inicio');
}

function volverALogin(aviso = 'Sesión finalizada. El token JWT fue eliminado de memoria.') {
  tokenActual = '';
  usuarioActual = '';
  catalogoVehiculos = [];
  listaCompradores = [];
  campoToken.value = '';
  campoToken.type = 'password';
  inputJwtLoginManual.value = '';
  inputContrasena.value = '';
  botonMostrar.textContent = 'Mostrar';
  botonMostrar.setAttribute('aria-pressed', 'false');
  infoSesion.textContent = 'Estado: No autenticado';
  limpiarListado();
  actualizarKpis();
  selectVehiculo.innerHTML = '<option value="">-- Inicie sesión para cargar vehículos --</option>';
  selectComprador.innerHTML = '<option value="">-- Inicie sesión para cargar compradores --</option>';
  contenedorResultadoVenta.style.display = 'none';
  contenedorDetalleVenta.style.display = 'none';
  pantallaApp.hidden = true;
  pantallaLogin.hidden = false;
  mostrarMensaje(aviso, 'neutro');
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

function actualizarKpis() {
  const disp = catalogoVehiculos.filter((v) => String(v.estado || '').toUpperCase() === 'DISPONIBLE').length;
  const vend = catalogoVehiculos.filter((v) => String(v.estado || '').toUpperCase() === 'VENDIDO').length;
  statDisponibles.textContent = String(disp);
  statVendidos.textContent = String(vend);
  statCompradores.textContent = String(listaCompradores.length);
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
  const filtrados = filtroEstadoActual === 'TODOS'
    ? vehiculos
    : vehiculos.filter((v) => String(v.estado || '').toUpperCase() === filtroEstadoActual);

  contadorVehiculos.textContent = `${filtrados.length} vehículo${filtrados.length === 1 ? '' : 's'}`;

  if (filtrados.length === 0) {
    contenedorVehiculos.append(crearElemento('p', 'vacio', 'No hay vehículos para el filtro seleccionado.'));
    return;
  }

  for (const v of filtrados) {
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
    actualizarKpis();

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
    return true;
  } catch (error) {
    mostrarMensaje(error.message, 'error');
    return false;
  }
}

async function cargarCompradores() {
  try {
    const res = await listarCompradores(tokenActual);
    listaCompradores = res.compradores;
    actualizarKpis();

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

// Navegación por pestañas
for (const tab of tabsMenu) {
  tab.addEventListener('click', () => mostrarVista(tab.dataset.vista));
}
btnIrAVentas.addEventListener('click', () => mostrarVista('ventas'));
btnIrAAutos.addEventListener('click', () => mostrarVista('autos'));
btnRefrescarCatalogo.addEventListener('click', () => cargarVehiculos());
btnCerrarSesionTop.addEventListener('click', () => volverALogin());

// Filtros visuales de catálogo
for (const chip of botonesFiltro) {
  chip.addEventListener('click', () => {
    filtroEstadoActual = chip.dataset.filtro || 'TODOS';
    for (const b of botonesFiltro) {
      b.classList.toggle('activo', b === chip);
    }
    pintarListado(catalogoVehiculos);
  });
}

// Limpiar contraseña al cambiar de usuario en el selector
selectUsuario.addEventListener('change', () => {
  inputContrasena.value = '';
  inputContrasena.focus();
});

// Autocompletar precio al seleccionar vehículo
selectVehiculo.addEventListener('change', () => {
  const idSeleccionado = Number(selectVehiculo.value);
  const vehiculoEncontrado = catalogoVehiculos.find((v) => (v.idVehiculo ?? v.id) === idSeleccionado);
  if (vehiculoEncontrado && vehiculoEncontrado.precio) {
    inputPrecioVenta.value = vehiculoEncontrado.precio;
  }
});

// Login OAuth 2.0
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
  mostrarMensaje('Autenticando contra Spring Boot vía NGINX…', 'neutro');

  try {
    const res = await loginOAuth(usuario, contrasena);
    tokenActual = res.token;
    campoToken.value = tokenActual;
    campoToken.type = 'password';
    botonMostrar.textContent = 'Mostrar';
    botonMostrar.setAttribute('aria-pressed', 'false');
    infoSesion.textContent = `Sesión activa: ${usuario} | Scope: ${res.scope || 'vehiculos'} | Expiración: ${res.expiraEn}s (JWT en memoria)`;

    entrarEnApp(usuario);
    mostrarMensaje(`HTTP ${res.codigoHttp}: Bienvenido "${usuario}". Cargando inventario y compradores…`, 'exito');

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

// Login manual opcional desde el desplegable del Login
formLoginManual.addEventListener('submit', async (e) => {
  e.preventDefault();
  const tok = normalizarToken(inputJwtLoginManual.value);
  if (!tok) {
    mostrarMensaje('Pegue un token JWT válido para continuar.', 'error');
    return;
  }
  tokenActual = tok;
  campoToken.value = tok;
  mostrarMensaje('Validando token JWT contra NGINX…', 'neutro');
  const okVeh = await cargarVehiculos();
  if (!okVeh) {
    tokenActual = '';
    campoToken.value = '';
    return;
  }
  infoSesion.textContent = 'Sesión manual establecida con JWT provisto en memoria.';
  entrarEnApp('JWT manual');
  const compradoresOk = await cargarCompradores();
  asignarNuevoUuid();
  if (compradoresOk) {
    mostrarMensaje(`HTTP 200: ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
  }
});

// Recarga / actualización manual de token desde la pestaña Sesión
formularioToken.addEventListener('submit', async (e) => {
  e.preventDefault();
  tokenActual = normalizarToken(campoToken.value);
  if (!tokenActual) {
    mostrarMensaje('Por favor ingrese un token JWT válido o inicie sesión.', 'error');
    return;
  }
  infoSesion.textContent = 'Sesión manual actualizada con JWT en memoria.';
  mostrarMensaje('Actualizando catálogo y compradores…');
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

botonBorrar.addEventListener('click', () => volverALogin());

// Botones de Idempotency-Key
btnNuevoUuid.addEventListener('click', () => {
  const nuevo = asignarNuevoUuid();
  mostrarMensaje(`Nueva clave de idempotencia generada: ${nuevo}`, 'neutro');
});

btnClaveMisma.addEventListener('click', () => {
  mostrarMensaje(`Clave de idempotencia actual retenida para prueba de reintento: ${inputIdempotencyKey.value}`, 'neutro');
});

btnRecargarVehiculos.addEventListener('click', () => cargarVehiculos());
btnRecargarCompradores.addEventListener('click', () => cargarCompradores());

function formatearFechaLocal(valorIso) {
  if (!valorIso) return '—';
  const fecha = new Date(valorIso);
  if (Number.isNaN(fecha.getTime())) return String(valorIso);
  return fecha.toLocaleString('es-EC', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

function crearItemComprobante(etiqueta, valor, opciones = {}) {
  const item = crearElemento('div', `comprobante-item${opciones.ancho ? ' comprobante-item-ancho' : ''}`);
  item.append(crearElemento('span', 'comprobante-lbl', etiqueta));
  if (opciones.pill) {
    item.append(crearElemento('span', `pill-mini ${opciones.pill}`, valor));
  } else {
    item.append(crearElemento('span', `comprobante-val${opciones.mono ? ' mono' : ''}`, valor));
  }
  return item;
}

function pintarComprobanteVenta(contenedor, venta, codigoHttp, esConsulta = false) {
  contenedor.replaceChildren();
  const estado = String(venta?.estado || 'PROCESADA').toUpperCase();
  const notif = String(venta?.notificacion || 'PENDIENTE').toUpperCase();
  const esCompletada = estado === 'COMPLETADA';

  contenedor.className = `comprobante-venta ${esCompletada ? 'exito' : estado === 'FALLIDA' ? 'error' : 'conflicto'}`;

  const veh = catalogoVehiculos.find((v) => Number(v.idVehiculo ?? v.id) === Number(venta?.vehiculoId));
  const comp = listaCompradores.find((c) => Number(c.id) === Number(venta?.compradorId));
  const descVehiculo = veh
    ? `${texto(veh.marca)} ${texto(veh.modelo)} (${texto(veh.placa)}) · ID #${texto(venta?.vehiculoId)}`
    : `Vehículo ID #${texto(venta?.vehiculoId)}`;
  const descComprador = comp
    ? `${texto(comp.nombre)} (${texto(comp.documento || comp.cedula)}) · ID #${texto(venta?.compradorId)}`
    : `Comprador ID #${texto(venta?.compradorId)}`;

  const top = crearElemento('div', 'comprobante-top');
  const tituloWrap = crearElemento('div', 'comprobante-titulo-wrap');
  tituloWrap.append(crearElemento('div', `comprobante-icono ${esCompletada ? 'exito' : estado === 'FALLIDA' ? 'error' : 'conflicto'}`, esCompletada ? '✓' : '!'));

  const tituloTextos = crearElemento('div');
  const tituloPrincipal = esConsulta
    ? `Venta #${texto(venta?.id)} — ${estado}`
    : (codigoHttp === 200 ? `Venta #${texto(venta?.id)} recuperada (Idempotente)` : `¡Venta #${texto(venta?.id)} registrada con éxito!`);
  const subtitulo = esCompletada
    ? 'Vehículo marcado como VENDIDO y notificación enviada a RabbitMQ.'
    : `Estado actual en FastAPI: ${estado} · Notificación: ${notif}.`;
  tituloTextos.append(
    crearElemento('h3', 'comprobante-titulo', tituloPrincipal),
    crearElemento('p', 'comprobante-sub', subtitulo),
  );
  tituloWrap.append(tituloTextos);

  const montoBox = crearElemento('div', 'comprobante-monto');
  const precioMostrar = venta?.precioAcordado ?? venta?.precio;
  montoBox.append(
    crearElemento('span', 'comprobante-monto-valor', `${precio(precioMostrar)} ${texto(venta?.moneda || 'USD')}`),
    crearElemento('span', 'comprobante-monto-lbl', esCompletada ? 'Vehículo vendido' : estado),
  );
  top.append(tituloWrap, montoBox);

  const grilla = crearElemento('div', 'comprobante-grilla');
  grilla.append(
    crearItemComprobante('Vehículo', descVehiculo),
    crearItemComprobante('Comprador', descComprador),
    crearItemComprobante('Fecha y hora', formatearFechaLocal(venta?.fechaUTC || venta?.fecha)),
    crearItemComprobante('Estado de Venta', estado, { pill: esCompletada ? 'ok' : estado === 'FALLIDA' ? 'err' : 'warn' }),
    crearItemComprobante('Notificación RabbitMQ', notif, { pill: notif === 'ENVIADA' ? 'ok' : 'warn' }),
    crearItemComprobante('N.º de Registro', `Venta ID #${texto(venta?.id)} · HTTP ${codigoHttp || 200}`),
    crearItemComprobante('ID de Operación (UUID)', texto(venta?.operacionId), { mono: true, ancho: true }),
  );

  contenedor.append(top, grilla);
}

function pintarComprobanteError(contenedor, codigoHttp, mensajeError, detalles = {}) {
  contenedor.replaceChildren();
  const esConflicto = Number(codigoHttp) === 409;
  contenedor.className = `comprobante-venta ${esConflicto ? 'conflicto' : 'error'}`;

  const top = crearElemento('div', 'comprobante-top');
  const tituloWrap = crearElemento('div', 'comprobante-titulo-wrap');
  tituloWrap.append(crearElemento('div', `comprobante-icono ${esConflicto ? 'conflicto' : 'error'}`, '!'));

  const tituloTextos = crearElemento('div');
  const titulo = esConflicto
    ? 'Operación rechazada por conflicto (HTTP 409)'
    : `Operación no confirmada (HTTP ${codigoHttp || 'Error'})`;
  tituloTextos.append(
    crearElemento('h3', 'comprobante-titulo', titulo),
    crearElemento('p', 'comprobante-sub', detalles?.detail || mensajeError || 'No se completó la transacción.'),
  );
  tituloWrap.append(tituloTextos);
  top.append(tituloWrap);

  const grilla = crearElemento('div', 'comprobante-grilla');
  grilla.append(
    crearItemComprobante('Código', texto(detalles?.codigo || detalles?.title || `HTTP_${codigoHttp}`), { pill: esConflicto ? 'warn' : 'err' }),
    crearItemComprobante('Resultado', 'Sin venta confirmada'),
    crearItemComprobante('Trace ID', texto(detalles?.traceId || '—'), { mono: true }),
  );
  contenedor.append(top, grilla);
}

// Registro de Venta (lógica intacta, respuesta con comprobante visual)
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
    pintarComprobanteVenta(resumenVisualVenta, res.venta, res.codigoHttp, false);
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

    await cargarVehiculos();
  } catch (error) {
    contenedorResultadoVenta.style.display = 'block';
    const codigo = error.codigoHttp || 400;
    badgeEstadoVenta.className = codigo === 409 ? 'badge-http conflict' : 'badge-http error';
    badgeEstadoVenta.textContent = `HTTP ${codigo}`;
    pintarComprobanteError(resumenVisualVenta, codigo, error.message, error.detalles);
    codigoResultadoVenta.textContent = JSON.stringify(error.detalles || { error: error.message }, null, 2);
    mostrarMensaje(error.message, 'error');
  } finally {
    btnRegistrarVenta.disabled = false;
  }
});

// Consulta de Venta por ID (lógica intacta, respuesta con comprobante visual)
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
    pintarComprobanteVenta(resumenVisualDetalle, res.venta, res.codigoHttp || 200, true);
    codigoDetalleVenta.textContent = JSON.stringify(res.venta, null, 2);
    mostrarMensaje(`Consulta de venta ID ${idVenta} completada exitosamente.`, 'exito');
  } catch (error) {
    contenedorDetalleVenta.style.display = 'block';
    pintarComprobanteError(resumenVisualDetalle, error.codigoHttp || 404, error.message, error.detalles);
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

asignarNuevoUuid();

if ('serviceWorker' in navigator && window.isSecureContext) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}
