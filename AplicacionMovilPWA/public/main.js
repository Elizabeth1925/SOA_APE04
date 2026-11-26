/**
 * ============================================================================
 * Archivo: public/main.js
 * Propósito:
 *   Controlador principal de la interfaz y estado en memoria de la Aplicación
 *   Móvil PWA "AutoUTA". Orquesta la autenticación OAuth 2.0, la separación
 *   entre pantalla de Login y aplicación autenticada por pestañas, el registro
 *   exclusivo de ventas con `Idempotency-Key`, la consulta individual por ID,
 *   el renderizado de comprobantes visuales y tarjetas de catálogo con filtros
 *   rápidos, el monitoreo de conectividad y el registro del Service Worker.
 *
 * Actividades de la guía APE 04 implementadas:
 *   - [Actividad 6] Login OAuth 2.0 real (`POST /oauth/token`, `idapli=92`),
 *     JWT almacenado exclusivamente en memoria volátil (nunca en `localStorage`
 *     ni `sessionStorage`), emisión exclusiva de ventas (`POST /api/ventas` con
 *     `Idempotency-Key`), consulta posterior `GET /api/ventas/{id}` y registro
 *     del Service Worker (`/sw.js`).
 *   - [Actividad 10 - P27] Coherencia transversal con Windows Forms, React y
 *     JavaFX usando los mismos `vehiculoId`, `compradorId`, `ventaId` (`id`) y
 *     `operacionId` en formularios, catálogo y comprobantes.
 *   - [Actividad 11 - P08/P10/P11/P12/P13/503/504] Gestión fiel de errores
 *     401/403/404/409/502/503/504 sin simular ventas, soporte explícito de
 *     reintento idempotente P10 (HTTP 200), conflicto P11 (HTTP 409) y bloqueo
 *     visual/manejo de vehículos vendidos P12 (HTTP 409).
 *   - [Actividad 12 - Mejora Visual AutoUTA v5] Rediseño "AutoUTA v5 Minimalista
 *     Claro": pantalla de login dedicada que oculta ventas y catálogo antes de
 *     autenticar, navegación por pestañas (Inicio, Ventas, Autos, Sesión),
 *     KPIs del inventario, comprobante visual estructurado + desplegable JSON y
 *     filtros rápidos (Todos, Disponibles, Vendidos).
 * ============================================================================
 */

// [Actividad 6] Importa todas las operaciones del cliente HTTP OAuth 2.0, catálogo, compradores y ventas desde `api.js`.
import {
  normalizarToken,
  loginOAuth,
  listarVehiculos,
  listarCompradores,
  registrarVenta,
  consultarVenta,
} from './api.js?v=2';

// [Actividad 6] Estado en memoria volátil de la aplicación: jamás se escribe en `localStorage`, `sessionStorage`, cookies ni IndexedDB.
// [Actividad 6] Almacena el JWT Bearer activo solo mientras dure la pestaña en RAM.
let tokenActual = '';
// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Guarda el nombre del usuario autenticado para mostrarlo en la barra superior.
let usuarioActual = '';
// [Actividad 6] [Actividad 10 - P27] Caché en memoria del inventario de vehículos sincronizado con Spring Boot.
let catalogoVehiculos = [];
// [Actividad 6] [Actividad 10 - P27] Caché en memoria de compradores sincronizados con FastAPI.
let listaCompradores = [];
// [Actividad 12 - Mejora Visual AutoUTA v5] Estado del filtro rápido activo en la pestaña Autos (`TODOS`, `DISPONIBLE` o `VENDIDO`).
let filtroEstadoActual = 'TODOS';

// [Actividad 12 - Mejora Visual AutoUTA v5] Referencias del DOM para alternar entre Login separado y la App principal con pestañas.
// [Actividad 12 - Mejora Visual AutoUTA v5] Contenedor de la Pantalla 1 (Login minimalista previo a la autenticación).
const pantallaLogin = document.querySelector('#pantallaLogin');
// [Actividad 12 - Mejora Visual AutoUTA v5] Contenedor de la Pantalla 2 (Aplicación autenticada, oculta por defecto).
const pantallaApp = document.querySelector('#pantallaApp');
// [Actividad 12 - Mejora Visual AutoUTA v5] Botones de pestañas de la barra superior (Inicio, Ventas, Autos, Sesión).
const tabsMenu = document.querySelectorAll('.tab-link');
// [Actividad 12 - Mejora Visual AutoUTA v5] Paneles de contenido correspondientes a cada pestaña.
const vistasPanel = document.querySelectorAll('.vista-panel');
// [Actividad 12 - Mejora Visual AutoUTA v5] Etiqueta dentro del chip superior que muestra el nombre del usuario autenticado.
const nombreUsuarioChip = document.querySelector('#nombreUsuarioChip');
// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Botón superior "Salir" para limpiar el JWT de memoria y volver al login.
const btnCerrarSesionTop = document.querySelector('#btnCerrarSesionTop');
// [Actividad 12 - Mejora Visual AutoUTA v5] Botón CTA "Emitir venta ahora" del hero en Inicio.
const btnIrAVentas = document.querySelector('#btnIrAVentas');
// [Actividad 12 - Mejora Visual AutoUTA v5] Botón secundario "Ver catálogo" del hero en Inicio.
const btnIrAAutos = document.querySelector('#btnIrAAutos');
// [Actividad 6] Botón de refresco manual del inventario en la pestaña Autos.
const btnRefrescarCatalogo = document.querySelector('#btnRefrescarCatalogo');
// [Actividad 12 - Mejora Visual AutoUTA v5] Chips de filtro rápido (Todos / Disponibles / Vendidos).
const botonesFiltro = document.querySelectorAll('.chip-filtro');

// [Actividad 12 - Mejora Visual AutoUTA v5] Contadores KPI mostrados en la vista Inicio.
// [Actividad 12 - Mejora Visual AutoUTA v5] Contador de vehículos en estado `DISPONIBLE`.
const statDisponibles = document.querySelector('#statDisponibles');
// [Actividad 12 - Mejora Visual AutoUTA v5] Contador de vehículos en estado `VENDIDO`.
const statVendidos = document.querySelector('#statVendidos');
// [Actividad 12 - Mejora Visual AutoUTA v5] Contador total de compradores registrados.
const statCompradores = document.querySelector('#statCompradores');

// [Actividad 6] Referencias del DOM correspondientes al Login OAuth 2.0 y la pestaña Sesión.
// [Actividad 6] Formulario de inicio de sesión por usuario y contraseña.
const formLogin = document.querySelector('#formularioLogin');
// [Actividad 6] Selector de cuenta (`operador`, `admin`, `consulta`).
const selectUsuario = document.querySelector('#usuario');
// [Actividad 6] Campo de entrada de contraseña para el flujo `grant_type=password`.
const inputContrasena = document.querySelector('#contrasena');
// [Actividad 6] Botón de envío del formulario de login (se deshabilita durante la llamada HTTP).
const btnLogin = document.querySelector('#botonLogin');
// [Actividad 11] [Actividad 12 - Mejora Visual AutoUTA v5] Banner de estado/error visible dentro de la pantalla de Login.
const mensajeLogin = document.querySelector('#mensajeLogin');
// [Actividad 6] Formulario opcional en el desplegable para ingresar un JWT manualmente.
const formLoginManual = document.querySelector('#formularioLoginManual');
// [Actividad 6] Campo de texto enmascarado donde se pega el JWT manual en Login.
const inputJwtLoginManual = document.querySelector('#jwtLoginManual');

// [Actividad 6] Formulario de la pestaña Sesión para actualizar JWT en memoria y recargar datos.
const formularioToken = document.querySelector('#formulario');
// [Actividad 6] Campo que refleja el JWT activo en memoria dentro de la pestaña Sesión.
const campoToken = document.querySelector('#jwt');
// [Actividad 6] Botón para alternar entre ocultar y mostrar el JWT en pantalla.
const botonMostrar = document.querySelector('#alternarToken');
// [Actividad 6] Botón "Cerrar sesión" de la pestaña Sesión.
const botonBorrar = document.querySelector('#borrar');
// [Actividad 6] Contenedor que muestra el usuario activo, scope y expiración del JWT.
const infoSesion = document.querySelector('#infoSesion');
// [Actividad 11] [Actividad 12 - Mejora Visual AutoUTA v5] Banner global de mensajes operativos dentro de la App autenticada.
const mensaje = document.querySelector('#mensaje');

// [Actividad 6] Referencias del DOM para el formulario exclusivo de emisión de ventas (`POST /api/ventas`).
// [Actividad 6] Formulario principal de emisión de ventas.
const formVenta = document.querySelector('#formularioVenta');
// [Actividad 6] [Actividad 10 - P27] Selector de vehículos (`vehiculoId`).
const selectVehiculo = document.querySelector('#selectVehiculo');
// [Actividad 6] [Actividad 10 - P27] Selector de compradores (`compradorId`).
const selectComprador = document.querySelector('#selectComprador');
// [Actividad 6] Campo numérico del precio acordado de la venta.
const inputPrecioVenta = document.querySelector('#precioVenta');
// [Actividad 6] [Actividad 11 - P10/P11] Campo con el UUID v4 enviado en la cabecera `Idempotency-Key`.
const inputIdempotencyKey = document.querySelector('#idempotencyKey');
// [Actividad 6] [Actividad 11 - P10/P11] Botón que regenera un nuevo UUID v4 para una nueva operación.
const btnNuevoUuid = document.querySelector('#btnNuevoUuid');
// [Actividad 11 - P10/P11] Botón informativo que retiene la clave actual para probar reintento 200 o conflicto 409.
const btnClaveMisma = document.querySelector('#btnClaveMisma');
// [Actividad 6] Botón de envío de la venta (`POST /api/ventas`).
const btnRegistrarVenta = document.querySelector('#btnRegistrarVenta');
// [Actividad 6] Botón micro para recargar manualmente la lista de vehículos desde Spring Boot.
const btnRecargarVehiculos = document.querySelector('#btnRecargarVehiculos');
// [Actividad 6] Botón micro para recargar manualmente la lista de compradores desde FastAPI.
const btnRecargarCompradores = document.querySelector('#btnRecargarCompradores');

// [Actividad 12 - Mejora Visual AutoUTA v5] Contenedores del comprobante visual y JSON técnico tras registrar una venta.
// [Actividad 12 - Mejora Visual AutoUTA v5] Caja completa de resultado de la venta emitida.
const contenedorResultadoVenta = document.querySelector('#contenedorResultadoVenta');
// [Actividad 11] [Actividad 12 - Mejora Visual AutoUTA v5] Insignia con el código HTTP exacto de la respuesta (201, 200, 409, 403, etc.).
const badgeEstadoVenta = document.querySelector('#badgeEstadoVenta');
// [Actividad 12 - Mejora Visual AutoUTA v5] Contenedor del comprobante visual estructurado de venta/error.
const resumenVisualVenta = document.querySelector('#resumenVisualVenta');
// [Actividad 12 - Mejora Visual AutoUTA v5] Bloque `<pre>` dentro del desplegable con el JSON técnico íntegro.
const codigoResultadoVenta = document.querySelector('#codigoResultadoVenta');

// [Actividad 6] Referencias del DOM para la consulta posterior de una venta por su ID (`GET /api/ventas/{id}`).
// [Actividad 6] [Actividad 10 - P27] Campo numérico para ingresar el `ventaId` a consultar.
const inputIdVentaBuscar = document.querySelector('#idVentaBuscar');
// [Actividad 6] Botón que dispara `GET /api/ventas/{id}`.
const btnBuscarVenta = document.querySelector('#btnBuscarVenta');
// [Actividad 12 - Mejora Visual AutoUTA v5] Caja de resultado para la consulta individual de venta.
const contenedorDetalleVenta = document.querySelector('#contenedorDetalleVenta');
// [Actividad 12 - Mejora Visual AutoUTA v5] Comprobante visual estructurado de la venta consultada.
const resumenVisualDetalle = document.querySelector('#resumenVisualDetalle');
// [Actividad 12 - Mejora Visual AutoUTA v5] Bloque `<pre>` con la respuesta JSON cruda de `GET /api/ventas/{id}`.
const codigoDetalleVenta = document.querySelector('#codigoDetalleVenta');

// [Actividad 6] Referencias del DOM para el catálogo de vehículos y el indicador de estado de red.
// [Actividad 6] Contenedor en cuadrícula donde se insertan las tarjetas de vehículos.
const contenedorVehiculos = document.querySelector('#vehiculos');
// [Actividad 12 - Mejora Visual AutoUTA v5] Insignia numérica con la cantidad de vehículos visibles tras aplicar filtro.
const contadorVehiculos = document.querySelector('#contador');
// [Actividad 6] Indicador del pie de página que informa si el navegador está conectado o sin red.
const conexion = document.querySelector('#conexion');

// [Actividad 6] Formateador de moneda en dólares estadounidenses con formato regional de Ecuador (`es-EC`).
const formatoMoneda = new Intl.NumberFormat('es-EC', { style: 'currency', currency: 'USD' });

// [Actividad 11] [Actividad 12 - Mejora Visual AutoUTA v5] Muestra un mensaje de estado (`neutro`, `exito` o `error`) en la App y, si el Login está visible, también en el banner de Login.
function mostrarMensaje(texto, tipo = 'neutro') {
  // [Actividad 11] Actualiza el texto plano del banner principal de estado (seguro contra XSS al usar `textContent`).
  mensaje.textContent = texto;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Aplica la clase visual correspondiente al tipo de notificación.
  mensaje.className = `mensaje ${tipo}`;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Si el usuario aún está en la pantalla de Login, replica el aviso en `#mensajeLogin`.
  if (!pantallaLogin.hidden) {
    // [Actividad 12 - Mejora Visual AutoUTA v5] Hace visible el contenedor de notificación del Login.
    mensajeLogin.style.display = 'block';
    // [Actividad 11] Muestra el mensaje textual exacto (incluyendo códigos HTTP reales ante fallos).
    mensajeLogin.textContent = texto;
    // [Actividad 12 - Mejora Visual AutoUTA v5] Aplica el estilo semántico (`neutro`, `exito`, `error`).
    mensajeLogin.className = `mensaje ${tipo}`;
  }
}

// [Actividad 12 - Mejora Visual AutoUTA v5] Cambia la pestaña activa entre `inicio`, `ventas`, `autos` y `sesion`.
function mostrarVista(nombre) {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Resalta con subrayado la pestaña cuyo `data-vista` coincide con `nombre`.
  for (const tab of tabsMenu) {
    tab.classList.toggle('activa', tab.dataset.vista === nombre);
  }
  // [Actividad 12 - Mejora Visual AutoUTA v5] Muestra únicamente el panel cuyo ID coincide con `vista-<nombre>` y oculta los demás.
  for (const panel of vistasPanel) {
    const activa = panel.id === `vista-${nombre}`;
    panel.hidden = !activa;
    panel.classList.toggle('activa', activa);
  }
}

// [Actividad 12 - Mejora Visual AutoUTA v5] Transiciona de la pantalla dedicada de Login a la aplicación autenticada.
function entrarEnApp(nombreUsuario) {
  // [Actividad 6] Guarda el identificador de usuario en memoria o usa una etiqueta por defecto para sesiones con JWT manual.
  usuarioActual = nombreUsuario || 'sesión JWT';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Actualiza el chip de usuario en la barra superior.
  nombreUsuarioChip.textContent = usuarioActual;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Oculta la pantalla de Login.
  pantallaLogin.hidden = true;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Revela la aplicación principal autenticada.
  pantallaApp.hidden = false;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Oculta cualquier alerta previa mostrada en el Login.
  mensajeLogin.style.display = 'none';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Ubica al usuario en la pestaña "Inicio" al ingresar.
  mostrarVista('inicio');
}

// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Cierra sesión limpiando completamente el JWT y datos de negocio de la memoria RAM y regresa al Login.
function volverALogin(aviso = 'Sesión finalizada. El token JWT fue eliminado de memoria.') {
  // [Actividad 6] Purga el JWT de la variable en memoria volátil.
  tokenActual = '';
  // [Actividad 6] Limpia el nombre de usuario activo.
  usuarioActual = '';
  // [Actividad 6] Vacía la caché en memoria del catálogo de vehículos.
  catalogoVehiculos = [];
  // [Actividad 6] Vacía la caché en memoria de compradores.
  listaCompradores = [];
  // [Actividad 6] Limpia el valor del campo JWT en la pestaña Sesión.
  campoToken.value = '';
  // [Actividad 6] Restaura el tipo `password` para mantener enmascarado el campo JWT.
  campoToken.type = 'password';
  // [Actividad 6] Limpia el campo de ingreso manual de JWT del Login.
  inputJwtLoginManual.value = '';
  // [Actividad 6] Limpia el campo de contraseña para no retener credenciales en el DOM.
  inputContrasena.value = '';
  // [Actividad 6] Restaura la etiqueta del botón alternador a "Mostrar".
  botonMostrar.textContent = 'Mostrar';
  // [Actividad 6] Actualiza el estado accesible `aria-pressed` a `false`.
  botonMostrar.setAttribute('aria-pressed', 'false');
  // [Actividad 6] Restablece el resumen de estado en la pestaña Sesión.
  infoSesion.textContent = 'Estado: No autenticado';
  // [Actividad 6] Borra las tarjetas renderizadas del catálogo de vehículos.
  limpiarListado();
  // [Actividad 12 - Mejora Visual AutoUTA v5] Reinicia a cero los contadores KPI del Inicio.
  actualizarKpis();
  // [Actividad 6] Restaura las opciones por defecto de los selectores de venta.
  selectVehiculo.innerHTML = '<option value="">-- Inicie sesión para cargar vehículos --</option>';
  selectComprador.innerHTML = '<option value="">-- Inicie sesión para cargar compradores --</option>';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Oculta comprobantes de ventas y consultas previas.
  contenedorResultadoVenta.style.display = 'none';
  contenedorDetalleVenta.style.display = 'none';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Oculta toda la pantalla operativa para proteger vistas de negocio sin autenticación.
  pantallaApp.hidden = true;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Vuelve a mostrar la pantalla dedicada de Login.
  pantallaLogin.hidden = false;
  // [Actividad 6] Notifica al usuario que la sesión fue cerrada y el token purgado.
  mostrarMensaje(aviso, 'neutro');
}

// [Actividad 6] [Actividad 11 - P10/P11] Genera un identificador UUID v4 único para la cabecera `Idempotency-Key` de `POST /api/ventas`.
function generarUuid() {
  // [Actividad 6] Usa la API criptográfica nativa `crypto.randomUUID()` cuando el navegador la provee.
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID();
  }
  // [Actividad 6] Respaldo compatible RFC 4122 v4 para contextos donde `crypto.randomUUID` no esté disponible.
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// [Actividad 6] [Actividad 11 - P10/P11] Genera un nuevo UUID v4 y lo asigna directamente al campo `idempotencyKey`.
function asignarNuevoUuid() {
  // [Actividad 6] Obtiene un identificador único nuevo.
  const nuevoUuid = generarUuid();
  // [Actividad 6] Lo escribe en el input de Idempotency-Key del formulario de ventas.
  inputIdempotencyKey.value = nuevoUuid;
  // [Actividad 6] Retorna el valor generado para poder mostrarlo en mensajes informativos.
  return nuevoUuid;
}

// [Actividad 6] Devuelve un guion largo `—` cuando un campo es nulo, indefinido o vacío, o su representación en texto.
function texto(valor) {
  return valor === null || valor === undefined || valor === '' ? '—' : String(valor);
}

// [Actividad 6] Formatea valores monetarios como moneda USD (`es-EC`), protegiendo ante valores vacíos o no numéricos.
function precio(valor) {
  // [Actividad 6] Retorna guion largo si no hay monto definido.
  if (valor === null || valor === undefined || valor === '') return '—';
  // [Actividad 6] Convierte el valor a número flotante para validar si es finito.
  const numero = Number(valor);
  // [Actividad 6] Aplica formato USD si es numérico válido; de lo contrario, lo muestra como texto seguro.
  return Number.isFinite(numero) ? formatoMoneda.format(numero) : texto(valor);
}

// [Actividad 12 - Mejora Visual AutoUTA v5] Recalcula y pinta los 3 mini KPIs de la vista Inicio (Disponibles, Vendidos, Compradores).
function actualizarKpis() {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Cuenta vehículos cuyo estado normalizado sea `DISPONIBLE`.
  const disp = catalogoVehiculos.filter((v) => String(v.estado || '').toUpperCase() === 'DISPONIBLE').length;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Cuenta vehículos cuyo estado normalizado sea `VENDIDO`.
  const vend = catalogoVehiculos.filter((v) => String(v.estado || '').toUpperCase() === 'VENDIDO').length;
  // [Actividad 12 - Mejora Visual AutoUTA v5] Refleja el conteo de disponibles en el DOM.
  statDisponibles.textContent = String(disp);
  // [Actividad 12 - Mejora Visual AutoUTA v5] Refleja el conteo de vendidos en el DOM.
  statVendidos.textContent = String(vend);
  // [Actividad 12 - Mejora Visual AutoUTA v5] Refleja el total de compradores cargados desde FastAPI.
  statCompradores.textContent = String(listaCompradores.length);
}

// [Actividad 6] Vacía el contenedor de tarjetas del catálogo y restablece el contador a `0 vehículos`.
function limpiarListado() {
  // [Actividad 6] Elimina todos los nodos hijos de `#vehiculos` sin dejar referencias colgantes.
  contenedorVehiculos.replaceChildren();
  // [Actividad 12 - Mejora Visual AutoUTA v5] Reinicia la etiqueta contadora del catálogo.
  contadorVehiculos.textContent = '0 vehículos';
}

// [Actividad 6] Utilidad segura de creación de nodos DOM (usa `textContent`, evitando inyección HTML/XSS).
function crearElemento(etiqueta, clase, contenido) {
  // [Actividad 6] Crea el elemento HTML solicitado.
  const elemento = document.createElement(etiqueta);
  // [Actividad 6] Asigna las clases CSS si se proporcionaron.
  if (clase) elemento.className = clase;
  // [Actividad 6] Inserta el texto de forma segura cuando `contenido` está definido.
  if (contenido !== undefined) elemento.textContent = String(contenido);
  // [Actividad 6] Retorna el elemento listo para ser montado en el DOM.
  return elemento;
}

// [Actividad 6] Agrega un par etiqueta/valor dentro de la cuadrícula de especificaciones de una tarjeta de vehículo.
function agregarDato(grilla, etiqueta, valor, extra = '') {
  // [Actividad 6] Crea el contenedor del dato con clase adicional opcional (p. ej. `dato-precio`).
  const grupo = crearElemento('div', extra);
  // [Actividad 6] Inserta la etiqueta superior y el valor formateado.
  grupo.append(
    crearElemento('span', 'dato-etiqueta', etiqueta),
    crearElemento('span', 'dato-valor', valor),
  );
  // [Actividad 6] Añade el bloque a la grilla de la tarjeta.
  grilla.append(grupo);
}

// [Actividad 6] [Actividad 10 - P27] [Actividad 12 - Mejora Visual AutoUTA v5] Renderiza las tarjetas del catálogo de vehículos aplicando el filtro rápido seleccionado.
function pintarListado(vehiculos) {
  // [Actividad 6] Limpia cualquier renderizado previo del catálogo.
  limpiarListado();
  // [Actividad 12 - Mejora Visual AutoUTA v5] Filtra la lista según el chip activo (`TODOS`, `DISPONIBLE` o `VENDIDO`).
  const filtrados = filtroEstadoActual === 'TODOS'
    ? vehiculos
    : vehiculos.filter((v) => String(v.estado || '').toUpperCase() === filtroEstadoActual);

  // [Actividad 12 - Mejora Visual AutoUTA v5] Actualiza el contador pluralizando correctamente ("1 vehículo" vs "N vehículos").
  contadorVehiculos.textContent = `${filtrados.length} vehículo${filtrados.length === 1 ? '' : 's'}`;

  // [Actividad 12 - Mejora Visual AutoUTA v5] Muestra estado vacío amigable si ningún vehículo coincide con el filtro elegido.
  if (filtrados.length === 0) {
    contenedorVehiculos.append(crearElemento('p', 'vacio', 'No hay vehículos para el filtro seleccionado.'));
    return;
  }

  // [Actividad 6] [Actividad 10 - P27] Construye una tarjeta por cada vehículo real devuelto por Spring Boot.
  for (const v of filtrados) {
    // [Actividad 12 - Mejora Visual AutoUTA v5] Tarjeta contenedora `<article class="vehiculo">`.
    const tarjeta = crearElemento('article', 'vehiculo');
    // [Actividad 12 - Mejora Visual AutoUTA v5] Fila superior con marca/modelo/ID a la izquierda e insignia de estado a la derecha.
    const cabecera = crearElemento('div', 'vehiculo-cabecera');
    // [Actividad 6] Agrupador de título e identificador del vehículo.
    const encabezado = crearElemento('div');
    // [Actividad 10 - P27] Normaliza la lectura del identificador transversal (`idVehiculo` de Spring Boot o `id`).
    const idVeh = v.idVehiculo ?? v.id;
    // [Actividad 10 - P27] Agrega título (marca y modelo) e ID visible para verificación cruzada con Windows Forms, React y JavaFX.
    encabezado.append(
      crearElemento('h3', 'vehiculo-titulo', `${texto(v.marca)} ${texto(v.modelo)}`),
      crearElemento('span', 'vehiculo-id', `ID: ${texto(idVeh)}`),
    );

    // [Actividad 6] Obtiene el texto del estado del vehículo (`DISPONIBLE` / `VENDIDO`).
    const estado = texto(v.estado);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Determina la clase semántica verde (`disponible`) o ámbar (`vendido`) para la píldora de estado.
    const claseEstado = String(estado).toUpperCase() === 'DISPONIBLE' ? 'disponible'
      : String(estado).toUpperCase() === 'VENDIDO' ? 'vendido' : '';
    // [Actividad 12 - Mejora Visual AutoUTA v5] Inserta encabezado y píldora de estado en la cabecera de la tarjeta.
    cabecera.append(encabezado, crearElemento('span', `estado ${claseEstado}`, estado));

    // [Actividad 6] Crea la grilla de 2 columnas con los atributos técnicos y comerciales del vehículo.
    const grilla = crearElemento('div', 'datos');
    // [Actividad 6] Muestra la placa del vehículo.
    agregarDato(grilla, 'Placa', texto(v.placa));
    // [Actividad 6] Muestra el año de fabricación.
    agregarDato(grilla, 'Año', texto(v.anio));
    // [Actividad 6] Muestra el color registrado.
    agregarDato(grilla, 'Color', texto(v.color));
    // [Actividad 6] Muestra el precio formateado en USD con énfasis tipográfico.
    agregarDato(grilla, 'Precio', precio(v.precio), 'dato-precio');
    // [Actividad 6] Ensambla cabecera y especificaciones dentro de la tarjeta.
    tarjeta.append(cabecera, grilla);
    // [Actividad 6] Monta la tarjeta en el contenedor principal `#vehiculos`.
    contenedorVehiculos.append(tarjeta);
  }
}

// [Actividad 6] [Actividad 10 - P27] [Actividad 11 - P12] Obtiene vehículos reales desde `/api/vehiculos`, actualiza catálogo, KPIs y selector de ventas.
async function cargarVehiculos() {
  try {
    // [Actividad 6] Invoca `GET /api/vehiculos` usando el JWT vigente en memoria volátil.
    const res = await listarVehiculos(tokenActual);
    // [Actividad 6] Actualiza la lista en memoria con los datos reales devueltos por Spring Boot.
    catalogoVehiculos = res.vehiculos;
    // [Actividad 12 - Mejora Visual AutoUTA v5] Renderiza las tarjetas del catálogo respetando el filtro rápido activo.
    pintarListado(catalogoVehiculos);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Recalcula los KPIs de Disponibles y Vendidos en la pestaña Inicio.
    actualizarKpis();

    // [Actividad 6] Reconstruye desde cero las opciones del selector de vehículos del formulario de ventas.
    selectVehiculo.replaceChildren();
    // [Actividad 6] Crea la opción inicial vacía que obliga a elegir explícitamente un vehículo.
    const opcionVacia = document.createElement('option');
    opcionVacia.value = '';
    opcionVacia.textContent = '-- Seleccione un vehículo disponible --';
    selectVehiculo.append(opcionVacia);

    // [Actividad 10 - P27] Pobla el selector mostrando `[ID ...]`, marca, modelo, placa, precio y estado actual.
    for (const v of catalogoVehiculos) {
      // [Actividad 10 - P27] Usa el mismo `vehiculoId` (`idVehiculo` o `id`) compartido en toda la arquitectura SOA.
      const id = v.idVehiculo ?? v.id;
      const opt = document.createElement('option');
      opt.value = id;
      const est = (v.estado || '').toUpperCase();
      opt.textContent = `[ID ${id}] ${v.marca} ${v.modelo} (${v.placa}) - ${precio(v.precio)} [${est}]`;
      // [Actividad 11 - P12] Bloquea en la UI la selección de vehículos ya vendidos (complementando el rechazo HTTP 409 del backend).
      if (est === 'VENDIDO') {
        opt.disabled = true;
        opt.textContent += ' (No disponible)';
      }
      selectVehiculo.append(opt);
    }
    // [Actividad 6] Indica carga exitosa al flujo llamador.
    return true;
  } catch (error) {
    // [Actividad 11 - P08/503/504] Informa en pantalla el error HTTP o de red real sin mostrar inventario ficticio.
    mostrarMensaje(error.message, 'error');
    // [Actividad 11] Indica que la carga falló (p. ej. para abortar login manual con JWT inválido).
    return false;
  }
}

// [Actividad 6] [Actividad 10 - P27] Obtiene compradores reales desde `/api/compradores` (FastAPI vía NGINX) y pobla el selector de ventas.
async function cargarCompradores() {
  try {
    // [Actividad 6] Consulta `GET /api/compradores` adjuntando el JWT de memoria.
    const res = await listarCompradores(tokenActual);
    // [Actividad 6] Almacena la lista de compradores en memoria volátil.
    listaCompradores = res.compradores;
    // [Actividad 12 - Mejora Visual AutoUTA v5] Actualiza el KPI de cantidad de compradores en la pestaña Inicio.
    actualizarKpis();

    // [Actividad 6] Limpia opciones previas del selector de compradores.
    selectComprador.replaceChildren();
    // [Actividad 6] Agrega opción placeholder indicando cuántos compradores están disponibles.
    const opcionVacia = document.createElement('option');
    opcionVacia.value = '';
    opcionVacia.textContent = listaCompradores.length > 0
      ? `-- Seleccione un comprador (${listaCompradores.length} disponibles) --`
      : '-- No hay compradores registrados --';
    selectComprador.append(opcionVacia);

    // [Actividad 10 - P27] Crea una opción por cada comprador mostrando su `compradorId`, nombre y documento de identidad.
    for (const c of listaCompradores) {
      const opt = document.createElement('option');
      opt.value = c.id;
      opt.textContent = `[ID ${c.id}] ${c.nombre} (${c.documento || c.cedula || c.identificacion || 'S/D'})`;
      selectComprador.append(opt);
    }
    // [Actividad 6] Retorna `true` confirmando carga satisfactoria.
    return true;
  } catch (error) {
    // [Actividad 11 - 503/504] Reemplaza el selector con un aviso explícito del código HTTP o indisponibilidad sin inventar compradores.
    selectComprador.replaceChildren();
    const optErr = document.createElement('option');
    optErr.value = '';
    optErr.textContent = `-- Error al cargar compradores (${error.codigoHttp ? 'HTTP ' + error.codigoHttp : 'Sin conexión'}) --`;
    selectComprador.append(optErr);
    // [Actividad 11 - 503/504] Muestra el error detallado en el banner de estado.
    mostrarMensaje(`Compradores: ${error.message}`, 'error');
    // [Actividad 11] Retorna `false` indicando error en la sincronización de compradores.
    return false;
  }
}

// [Actividad 12 - Mejora Visual AutoUTA v5] Conecta los eventos de clic de las pestañas de navegación (Inicio, Ventas, Autos, Sesión).
for (const tab of tabsMenu) {
  tab.addEventListener('click', () => mostrarVista(tab.dataset.vista));
}
// [Actividad 12 - Mejora Visual AutoUTA v5] Navegación directa desde el hero de Inicio hacia la vista de Emisión de Ventas.
btnIrAVentas.addEventListener('click', () => mostrarVista('ventas'));
// [Actividad 12 - Mejora Visual AutoUTA v5] Navegación directa desde el hero de Inicio hacia la vista de Catálogo de Autos.
btnIrAAutos.addEventListener('click', () => mostrarVista('autos'));
// [Actividad 6] Recarga en línea del inventario desde el botón Actualizar de la pestaña Autos.
btnRefrescarCatalogo.addEventListener('click', () => cargarVehiculos());
// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Cierra la sesión activa desde el botón "Salir" de la barra superior.
btnCerrarSesionTop.addEventListener('click', () => volverALogin());

// [Actividad 12 - Mejora Visual AutoUTA v5] Gestiona los chips de filtrado rápido (`Todos`, `Disponibles`, `Vendidos`) en la pestaña Autos.
for (const chip of botonesFiltro) {
  chip.addEventListener('click', () => {
    // [Actividad 12 - Mejora Visual AutoUTA v5] Actualiza el filtro seleccionado leyendo `data-filtro`.
    filtroEstadoActual = chip.dataset.filtro || 'TODOS';
    // [Actividad 12 - Mejora Visual AutoUTA v5] Marca visualmente como activo solo el chip pulsado.
    for (const b of botonesFiltro) {
      b.classList.toggle('activo', b === chip);
    }
    // [Actividad 12 - Mejora Visual AutoUTA v5] Vuelve a pintar las tarjetas aplicando el nuevo filtro sin llamadas innecesarias a red.
    pintarListado(catalogoVehiculos);
  });
}

// [Actividad 6] Limpia la contraseña y devuelve el foco al campo cada vez que se cambia la cuenta seleccionada en el Login.
selectUsuario.addEventListener('change', () => {
  inputContrasena.value = '';
  inputContrasena.focus();
});

// [Actividad 6] Autocompleta el precio acordado con el precio base del vehículo elegido en el formulario de ventas.
selectVehiculo.addEventListener('change', () => {
  // [Actividad 10 - P27] Convierte a número el `vehiculoId` seleccionado.
  const idSeleccionado = Number(selectVehiculo.value);
  // [Actividad 10 - P27] Busca el vehículo correspondiente en el inventario en memoria.
  const vehiculoEncontrado = catalogoVehiculos.find((v) => (v.idVehiculo ?? v.id) === idSeleccionado);
  // [Actividad 6] Precarga el precio de lista para agilizar el registro comercial (el operador puede ajustarlo antes de enviar).
  if (vehiculoEncontrado && vehiculoEncontrado.precio) {
    inputPrecioVenta.value = vehiculoEncontrado.precio;
  }
});

// [Actividad 6] [Actividad 11 - P08] [Actividad 12 - Mejora Visual AutoUTA v5] Manejador de envío del formulario de Login OAuth 2.0 real.
formLogin.addEventListener('submit', async (e) => {
  // [Actividad 6] Evita la recarga nativa de página del formulario HTML.
  e.preventDefault();
  // [Actividad 6] Lee el perfil seleccionado (`operador`, `admin` o `consulta`).
  const usuario = selectUsuario.value;
  // [Actividad 6] Lee la contraseña ingresada por el usuario.
  const contrasena = inputContrasena.value;

  // [Actividad 6] Valida localmente que la contraseña no esté vacía antes de llamar a `/oauth/token`.
  if (!contrasena.trim()) {
    mostrarMensaje('Ingrese la contraseña de la cuenta para iniciar sesión.', 'error');
    inputContrasena.focus();
    return;
  }

  // [Actividad 6] Deshabilita el botón mientras dura la solicitud para impedir envíos duplicados.
  btnLogin.disabled = true;
  // [Actividad 6] Informa al usuario que se está autenticando contra Spring Boot a través de NGINX.
  mostrarMensaje('Autenticando contra Spring Boot vía NGINX…', 'neutro');

  try {
    // [Actividad 6] Solicita un JWT real a `POST /oauth/token` (`idapli=92`).
    const res = await loginOAuth(usuario, contrasena);
    // [Actividad 6] Almacena el JWT recibido ÚNICAMENTE en la variable en memoria volátil `tokenActual`.
    tokenActual = res.token;
    // [Actividad 6] Sincroniza el campo de la pestaña Sesión para inspección controlada.
    campoToken.value = tokenActual;
    // [Actividad 6] Mantiene el token enmascarado por defecto.
    campoToken.type = 'password';
    botonMostrar.textContent = 'Mostrar';
    botonMostrar.setAttribute('aria-pressed', 'false');
    // [Actividad 6] Muestra metadatos de la sesión OAuth 2.0 (usuario, scope, expiración en segundos) en la pestaña Sesión.
    infoSesion.textContent = `Sesión activa: ${usuario} | Scope: ${res.scope || 'vehiculos'} | Expiración: ${res.expiraEn}s (JWT en memoria)`;

    // [Actividad 12 - Mejora Visual AutoUTA v5] Oculta la pantalla de Login y despliega la aplicación principal.
    entrarEnApp(usuario);
    // [Actividad 6] Notifica éxito de autenticación mientras se cargan vehículos y compradores reales.
    mostrarMensaje(`HTTP ${res.codigoHttp}: Bienvenido "${usuario}". Cargando inventario y compradores…`, 'exito');

    // [Actividad 6] [Actividad 10 - P27] Sincroniza el catálogo de vehículos desde Spring Boot.
    await cargarVehiculos();
    // [Actividad 6] [Actividad 10 - P27] Sincroniza el listado de compradores desde FastAPI.
    const compradoresOk = await cargarCompradores();
    // [Actividad 6] Prepara una nueva clave `Idempotency-Key` lista para la primera venta de la sesión.
    asignarNuevoUuid();
    // [Actividad 6] Si ambos servicios respondieron bien, muestra el resumen final de carga en el banner.
    if (compradoresOk) {
      mostrarMensaje(`HTTP ${res.codigoHttp}: Sesión activa como "${usuario}". ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
    }
  } catch (error) {
    // [Actividad 11 - P08/503/504] Ante credenciales inválidas o gateway caído, permanece en Login y reporta el error HTTP real.
    infoSesion.textContent = 'Estado: Error de autenticación';
    mostrarMensaje(error.message, 'error');
  } finally {
    // [Actividad 6] Rehabilita el botón de login al finalizar la operación.
    btnLogin.disabled = false;
  }
});

// [Actividad 6] [Actividad 11 - P08] [Actividad 12 - Mejora Visual AutoUTA v5] Manejador de ingreso opcional con un JWT pegado manualmente desde el desplegable del Login.
formLoginManual.addEventListener('submit', async (e) => {
  // [Actividad 6] Evita el envío tradicional del formulario.
  e.preventDefault();
  // [Actividad 6] Saneamiento del token pegado (quita espacios y prefijo `Bearer`).
  const tok = normalizarToken(inputJwtLoginManual.value);
  // [Actividad 6] Verifica que el usuario haya proporcionado un token no vacío.
  if (!tok) {
    mostrarMensaje('Pegue un token JWT válido para continuar.', 'error');
    return;
  }
  // [Actividad 6] Coloca temporalmente el token en memoria para validarlo contra `/api/vehiculos`.
  tokenActual = tok;
  campoToken.value = tok;
  // [Actividad 6] Informa que el JWT será verificado contra el gateway NGINX.
  mostrarMensaje('Validando token JWT contra NGINX…', 'neutro');
  // [Actividad 11 - P08] Valida realmente el JWT consultando `/api/vehiculos`; si el backend devuelve 401/403, rechaza el ingreso.
  const okVeh = await cargarVehiculos();
  if (!okVeh) {
    // [Actividad 11 - P08] Limpia el token rechazado de memoria para no dejar sesión inválida.
    tokenActual = '';
    campoToken.value = '';
    return;
  }
  // [Actividad 6] Registra en la pestaña Sesión que el acceso se realizó mediante JWT manual en memoria.
  infoSesion.textContent = 'Sesión manual establecida con JWT provisto en memoria.';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Abre la pantalla principal de la PWA tras confirmar validez del token.
  entrarEnApp('JWT manual');
  // [Actividad 6] Carga la lista de compradores desde FastAPI.
  const compradoresOk = await cargarCompradores();
  // [Actividad 6] Asigna un UUID v4 inicial para ventas.
  asignarNuevoUuid();
  // [Actividad 6] Confirma en el banner la carga completa de datos.
  if (compradoresOk) {
    mostrarMensaje(`HTTP 200: ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
  }
});

// [Actividad 6] Permite actualizar el JWT en memoria y recargar catálogo + compradores desde la pestaña Sesión.
formularioToken.addEventListener('submit', async (e) => {
  // [Actividad 6] Detiene el envío nativo del formulario.
  e.preventDefault();
  // [Actividad 6] Normaliza y actualiza el JWT en memoria volátil.
  tokenActual = normalizarToken(campoToken.value);
  // [Actividad 11 - P08] Evita operar si el usuario dejó el campo vacío.
  if (!tokenActual) {
    mostrarMensaje('Por favor ingrese un token JWT válido o inicie sesión.', 'error');
    return;
  }
  // [Actividad 6] Actualiza la descripción del estado de sesión.
  infoSesion.textContent = 'Sesión manual actualizada con JWT en memoria.';
  mostrarMensaje('Actualizando catálogo y compradores…');
  // [Actividad 6] Vuelve a consultar inventario y compradores usando el JWT actualizado.
  await cargarVehiculos();
  const compradoresOk = await cargarCompradores();
  if (compradoresOk && catalogoVehiculos.length > 0) {
    mostrarMensaje(`HTTP 200: ${catalogoVehiculos.length} vehículos y ${listaCompradores.length} compradores cargados.`, 'exito');
  }
});

// [Actividad 6] Alterna la visibilidad en texto plano del campo JWT en la pestaña Sesión con soporte accesible `aria-pressed`.
botonMostrar.addEventListener('click', () => {
  const revelar = campoToken.type === 'password';
  campoToken.type = revelar ? 'text' : 'password';
  botonMostrar.textContent = revelar ? 'Ocultar' : 'Mostrar';
  botonMostrar.setAttribute('aria-pressed', String(revelar));
});

// [Actividad 6] Conecta el botón "Cerrar sesión" de la pestaña Sesión con la purga total de memoria y retorno al Login.
botonBorrar.addEventListener('click', () => volverALogin());

// [Actividad 6] [Actividad 11 - P10/P11] Botón "Nuevo UUID": genera una nueva clave `Idempotency-Key` para una venta distinta.
btnNuevoUuid.addEventListener('click', () => {
  const nuevo = asignarNuevoUuid();
  mostrarMensaje(`Nueva clave de idempotencia generada: ${nuevo}`, 'neutro');
});

// [Actividad 11 - P10/P11] Botón "Fijar Clave": mantiene la clave vigente para ejecutar pruebas de reintento idempotente (P10 -> 200) o conflicto por payload distinto (P11 -> 409).
btnClaveMisma.addEventListener('click', () => {
  mostrarMensaje(`Clave de idempotencia actual retenida para prueba de reintento: ${inputIdempotencyKey.value}`, 'neutro');
});

// [Actividad 6] Botones de recarga rápida dentro del formulario de ventas para sincronizar vehículos y compradores sin cambiar de pestaña.
btnRecargarVehiculos.addEventListener('click', () => cargarVehiculos());
btnRecargarCompradores.addEventListener('click', () => cargarCompradores());

// [Actividad 12 - Mejora Visual AutoUTA v5] Convierte marcas de tiempo ISO/UTC recibidas desde FastAPI a formato local legible (`es-EC`).
function formatearFechaLocal(valorIso) {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Devuelve guion largo si no existe fecha.
  if (!valorIso) return '—';
  // [Actividad 12 - Mejora Visual AutoUTA v5] Intenta construir un objeto `Date` válido.
  const fecha = new Date(valorIso);
  // [Actividad 12 - Mejora Visual AutoUTA v5] Si la cadena no es parseable como fecha, la devuelve como texto sin fallar.
  if (Number.isNaN(fecha.getTime())) return String(valorIso);
  // [Actividad 12 - Mejora Visual AutoUTA v5] Formatea en fecha y hora local a dos dígitos.
  return fecha.toLocaleString('es-EC', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit',
  });
}

// [Actividad 12 - Mejora Visual AutoUTA v5] Construye una celda individual del comprobante visual (soporta ancho completo, estilo monoespaciado o píldora de estado).
function crearItemComprobante(etiqueta, valor, opciones = {}) {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Crea el bloque `.comprobante-item` (expandido a todas las columnas si `opciones.ancho` es verdadero).
  const item = crearElemento('div', `comprobante-item${opciones.ancho ? ' comprobante-item-ancho' : ''}`);
  // [Actividad 12 - Mejora Visual AutoUTA v5] Agrega la etiqueta descriptiva superior en mayúsculas pequeñas.
  item.append(crearElemento('span', 'comprobante-lbl', etiqueta));
  // [Actividad 12 - Mejora Visual AutoUTA v5] Renderiza como insignia `.pill-mini` o como valor textual (monoespaciado para UUID/Trace ID).
  if (opciones.pill) {
    item.append(crearElemento('span', `pill-mini ${opciones.pill}`, valor));
  } else {
    item.append(crearElemento('span', `comprobante-val${opciones.mono ? ' mono' : ''}`, valor));
  }
  // [Actividad 12 - Mejora Visual AutoUTA v5] Retorna la celda ensamblada.
  return item;
}

// [Actividad 12 - Mejora Visual AutoUTA v5] [Actividad 10 - P27] [Actividad 11 - P10] Renderiza el comprobante visual estructurado de una venta registrada o consultada.
function pintarComprobanteVenta(contenedor, venta, codigoHttp, esConsulta = false) {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Limpia el comprobante anterior antes de pintar el nuevo.
  contenedor.replaceChildren();
  // [Actividad 6] Normaliza el estado de la venta (`COMPLETADA`, `FALLIDA`, etc.) y de la notificación RabbitMQ (`ENVIADA`, `PENDIENTE`).
  const estado = String(venta?.estado || 'PROCESADA').toUpperCase();
  const notif = String(venta?.notificacion || 'PENDIENTE').toUpperCase();
  // [Actividad 12 - Mejora Visual AutoUTA v5] Determina si la transacción culminó completamente en éxito.
  const esCompletada = estado === 'COMPLETADA';

  // [Actividad 12 - Mejora Visual AutoUTA v5] Aplica la paleta visual de comprobante (`exito`, `error` o `conflicto`).
  contenedor.className = `comprobante-venta ${esCompletada ? 'exito' : estado === 'FALLIDA' ? 'error' : 'conflicto'}`;

  // [Actividad 10 - P27] Enriquecimiento visual local cruzando `vehiculoId` y `compradorId` de la venta con las listas en memoria.
  const veh = catalogoVehiculos.find((v) => Number(v.idVehiculo ?? v.id) === Number(venta?.vehiculoId));
  const comp = listaCompradores.find((c) => Number(c.id) === Number(venta?.compradorId));
  // [Actividad 10 - P27] Construye la descripción humana conservando siempre explícito el `vehiculoId` transversal.
  const descVehiculo = veh
    ? `${texto(veh.marca)} ${texto(veh.modelo)} (${texto(veh.placa)}) · ID #${texto(venta?.vehiculoId)}`
    : `Vehículo ID #${texto(venta?.vehiculoId)}`;
  // [Actividad 10 - P27] Construye la descripción del comprador conservando siempre explícito el `compradorId` transversal.
  const descComprador = comp
    ? `${texto(comp.nombre)} (${texto(comp.documento || comp.cedula)}) · ID #${texto(venta?.compradorId)}`
    : `Comprador ID #${texto(venta?.compradorId)}`;

  // [Actividad 12 - Mejora Visual AutoUTA v5] Crea la banda superior del comprobante con ícono, título y monto destacado.
  const top = crearElemento('div', 'comprobante-top');
  const tituloWrap = crearElemento('div', 'comprobante-titulo-wrap');
  // [Actividad 12 - Mejora Visual AutoUTA v5] Ícono circular verde (`✓`) para completada o alerta (`!`) para otros estados.
  tituloWrap.append(crearElemento('div', `comprobante-icono ${esCompletada ? 'exito' : estado === 'FALLIDA' ? 'error' : 'conflicto'}`, esCompletada ? '✓' : '!'));

  // [Actividad 12 - Mejora Visual AutoUTA v5] [Actividad 11 - P10] Distingue visualmente consulta (`GET`), reintento idempotente (`HTTP 200`) y creación nueva (`HTTP 201`).
  const tituloTextos = crearElemento('div');
  const tituloPrincipal = esConsulta
    ? `Venta #${texto(venta?.id)} — ${estado}`
    : (codigoHttp === 200 ? `Venta #${texto(venta?.id)} recuperada (Idempotente)` : `¡Venta #${texto(venta?.id)} registrada con éxito!`);
  // [Actividad 6] Subtítulo que confirma el cambio de estado a `VENDIDO` y la publicación de notificación en RabbitMQ.
  const subtitulo = esCompletada
    ? 'Vehículo marcado como VENDIDO y notificación enviada a RabbitMQ.'
    : `Estado actual en FastAPI: ${estado} · Notificación: ${notif}.`;
  tituloTextos.append(
    crearElemento('h3', 'comprobante-titulo', tituloPrincipal),
    crearElemento('p', 'comprobante-sub', subtitulo),
  );
  tituloWrap.append(tituloTextos);

  // [Actividad 12 - Mejora Visual AutoUTA v5] Bloque derecho con el monto total acordado y etiqueta del estado comercial.
  const montoBox = crearElemento('div', 'comprobante-monto');
  const precioMostrar = venta?.precioAcordado ?? venta?.precio;
  montoBox.append(
    crearElemento('span', 'comprobante-monto-valor', `${precio(precioMostrar)} ${texto(venta?.moneda || 'USD')}`),
    crearElemento('span', 'comprobante-monto-lbl', esCompletada ? 'Vehículo vendido' : estado),
  );
  top.append(tituloWrap, montoBox);

  // [Actividad 10 - P27] [Actividad 12 - Mejora Visual AutoUTA v5] Grilla detallada con vehículo, comprador, fecha, estado, notificación RabbitMQ, `ventaId` y `operacionId` (UUID).
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

  // [Actividad 12 - Mejora Visual AutoUTA v5] Inserta cabecera y grilla de detalles en el comprobante.
  contenedor.append(top, grilla);
}

// [Actividad 11 - P08/P11/P12/P13/503/504] [Actividad 12 - Mejora Visual AutoUTA v5] Renderiza un comprobante visual de rechazo/conflicto dejando explícito que NO se confirmó ninguna venta.
function pintarComprobanteError(contenedor, codigoHttp, mensajeError, detalles = {}) {
  // [Actividad 12 - Mejora Visual AutoUTA v5] Limpia el contenido previo del contenedor.
  contenedor.replaceChildren();
  // [Actividad 11 - P11/P12] Diferencia visualmente conflictos de negocio/idempotencia (HTTP 409 en ámbar) de otros errores (rojo).
  const esConflicto = Number(codigoHttp) === 409;
  contenedor.className = `comprobante-venta ${esConflicto ? 'conflicto' : 'error'}`;

  // [Actividad 12 - Mejora Visual AutoUTA v5] Cabecera del comprobante de rechazo con ícono de advertencia y título según código HTTP.
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

  // [Actividad 11] Grilla de auditoría de error: código de negocio, confirmación explícita "Sin venta confirmada" y `traceId` del servidor.
  const grilla = crearElemento('div', 'comprobante-grilla');
  grilla.append(
    crearItemComprobante('Código', texto(detalles?.codigo || detalles?.title || `HTTP_${codigoHttp}`), { pill: esConflicto ? 'warn' : 'err' }),
    crearItemComprobante('Resultado', 'Sin venta confirmada'),
    crearItemComprobante('Trace ID', texto(detalles?.traceId || '—'), { mono: true }),
  );
  // [Actividad 12 - Mejora Visual AutoUTA v5] Monta cabecera y grilla en el contenedor de resultado.
  contenedor.append(top, grilla);
}

// [Actividad 6] [Actividad 10 - P27] [Actividad 11 - P08/P10/P11/P12/P13/503/504] [Actividad 12 - Mejora Visual AutoUTA v5]
// Manejador principal de emisión de ventas (`POST /api/ventas` con `Idempotency-Key` y respuesta en comprobante visual + desplegable JSON).
formVenta.addEventListener('submit', async (e) => {
  // [Actividad 6] Previene el refresco de página por envío nativo.
  e.preventDefault();
  // [Actividad 10 - P27] Extrae `vehiculoId`, `compradorId`, `precioVenta` e `idempotencyKey` del formulario.
  const vehiculoId = Number(selectVehiculo.value);
  const compradorId = Number(selectComprador.value);
  const precioVenta = parseFloat(inputPrecioVenta.value);
  const idempotencyKey = inputIdempotencyKey.value.trim();

  // [Actividad 6] Valida que exista un vehículo seleccionado antes de emitir la transacción.
  if (!vehiculoId) {
    mostrarMensaje('Seleccione un vehículo disponible para la venta.', 'error');
    return;
  }
  // [Actividad 6] Valida que exista un comprador seleccionado.
  if (!compradorId) {
    mostrarMensaje('Seleccione un comprador para la venta.', 'error');
    return;
  }
  // [Actividad 6] Valida que el precio sea un número positivo mayor a cero.
  if (!precioVenta || precioVenta <= 0) {
    mostrarMensaje('Ingrese un precio de venta válido mayor a cero.', 'error');
    return;
  }
  // [Actividad 6] [Actividad 11 - P10/P11] Exige obligatoriamente la clave `Idempotency-Key`.
  if (!idempotencyKey) {
    mostrarMensaje('La clave Idempotency-Key es obligatoria.', 'error');
    return;
  }

  // [Actividad 6] Bloquea el botón durante el envío para evitar doble clic accidental.
  btnRegistrarVenta.disabled = true;
  // [Actividad 6] Informa al operador que la petición fue enviada hacia NGINX (`POST /api/ventas`).
  mostrarMensaje('Enviando transacción de venta a través de NGINX (POST /api/ventas)...');

  try {
    // [Actividad 10 - P27] Construye el contrato JSON exacto esperado por FastAPI (`vehiculoId`, `compradorId`, `precio`).
    const payload = {
      vehiculoId: vehiculoId,
      compradorId: compradorId,
      precio: precioVenta,
    };

    // [Actividad 6] [Actividad 11 - P10/P11/P12] Ejecuta `POST /api/ventas` enviando `Idempotency-Key` y JWT de memoria.
    const res = await registrarVenta(payload, idempotencyKey, tokenActual);

    // [Actividad 12 - Mejora Visual AutoUTA v5] Muestra la caja de resultado y actualiza el badge HTTP (`HTTP 201` o `HTTP 200` idempotente).
    contenedorResultadoVenta.style.display = 'block';
    badgeEstadoVenta.className = 'badge-http ok';
    badgeEstadoVenta.textContent = `HTTP ${res.codigoHttp}`;
    // [Actividad 12 - Mejora Visual AutoUTA v5] [Actividad 10 - P27] Pinta el comprobante visual estructurado con `ventaId`, `vehiculoId`, `compradorId` y `operacionId`.
    pintarComprobanteVenta(resumenVisualVenta, res.venta, res.codigoHttp, false);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Coloca el JSON técnico formateado dentro del desplegable `<details>`.
    codigoResultadoVenta.textContent = JSON.stringify(res.venta, null, 2);

    // [Actividad 6] [Actividad 10 - P27] Precarga automáticamente el `ventaId` emitido en el buscador de consulta posterior `GET /api/ventas/{id}`.
    if (res.venta?.id) {
      inputIdVentaBuscar.value = res.venta.id;
    }

    // [Actividad 6] Extrae el estado final y el estado de notificación RabbitMQ para el banner superior.
    const estadoTxt = res.venta?.estado || 'PROCESADA';
    const notifTxt = res.venta?.notificacion || 'PENDIENTE';
    mostrarMensaje(
      `HTTP ${res.codigoHttp}: Venta ID ${res.venta?.id ?? '—'} [${estadoTxt}] | Notificación RabbitMQ: ${notifTxt}.`,
      'exito',
    );

    // [Actividad 10 - P27] [Actividad 11 - P12] Recarga el catálogo de vehículos inmediatamente para reflejar el nuevo estado `VENDIDO` en KPIs, tarjetas y selector.
    await cargarVehiculos();
  } catch (error) {
    // [Actividad 11 - P08/P11/P12/P13/503/504] Muestra el rechazo real en el comprobante sin confirmar ni inventar ninguna venta.
    contenedorResultadoVenta.style.display = 'block';
    const codigo = error.codigoHttp || 400;
    // [Actividad 11 - P11/P12] Estiliza HTTP 409 como `conflict` (ámbar) y demás errores como `error` (rojo).
    badgeEstadoVenta.className = codigo === 409 ? 'badge-http conflict' : 'badge-http error';
    badgeEstadoVenta.textContent = `HTTP ${codigo}`;
    // [Actividad 12 - Mejora Visual AutoUTA v5] Pinta el comprobante estructurado de error/conflicto con `traceId`.
    pintarComprobanteError(resumenVisualVenta, codigo, error.message, error.detalles);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Expone el detalle JSON técnico del rechazo dentro del desplegable.
    codigoResultadoVenta.textContent = JSON.stringify(error.detalles || { error: error.message }, null, 2);
    // [Actividad 11] Replica el error HTTP en el banner global de estado.
    mostrarMensaje(error.message, 'error');
  } finally {
    // [Actividad 6] Rehabilita el botón de envío al concluir la transacción.
    btnRegistrarVenta.disabled = false;
  }
});

// [Actividad 6] [Actividad 10 - P27] [Actividad 11] [Actividad 12 - Mejora Visual AutoUTA v5]
// Manejador de consulta individual de una venta por su identificador (`GET /api/ventas/{id}`).
btnBuscarVenta.addEventListener('click', async () => {
  // [Actividad 6] Obtiene y recorta el `ventaId` ingresado por el usuario.
  const idVenta = inputIdVentaBuscar.value.trim();
  // [Actividad 6] Valida que se haya indicado un identificador antes de consultar.
  if (!idVenta) {
    mostrarMensaje('Ingrese un ID de venta válido para consultar.', 'error');
    return;
  }

  // [Actividad 6] Deshabilita temporalmente el botón mientras responde el backend.
  btnBuscarVenta.disabled = true;
  try {
    // [Actividad 6] [Actividad 10 - P27] Ejecuta `GET /api/ventas/{id}` usando el JWT en memoria.
    const res = await consultarVenta(idVenta, tokenActual);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Hace visible la tarjeta de detalle de consulta y pinta el comprobante en modo consulta (`esConsulta = true`).
    contenedorDetalleVenta.style.display = 'block';
    pintarComprobanteVenta(resumenVisualDetalle, res.venta, res.codigoHttp || 200, true);
    // [Actividad 12 - Mejora Visual AutoUTA v5] Inserta la respuesta JSON técnica en el desplegable inferior.
    codigoDetalleVenta.textContent = JSON.stringify(res.venta, null, 2);
    // [Actividad 6] Confirma en el banner la recuperación exitosa del registro.
    mostrarMensaje(`Consulta de venta ID ${idVenta} completada exitosamente.`, 'exito');
  } catch (error) {
    // [Actividad 11 - P08/P13/503/504] Muestra el comprobante de error real (p. ej. HTTP 404 si la venta no existe o 401/503).
    contenedorDetalleVenta.style.display = 'block';
    pintarComprobanteError(resumenVisualDetalle, error.codigoHttp || 404, error.message, error.detalles);
    codigoDetalleVenta.textContent = JSON.stringify({ error: error.message }, null, 2);
    mostrarMensaje(error.message, 'error');
  } finally {
    // [Actividad 6] Rehabilita el botón de búsqueda.
    btnBuscarVenta.disabled = false;
  }
});

// [Actividad 6] Refleja en tiempo real el estado de conectividad del navegador en el pie de página de la PWA.
function actualizarConexion() {
  conexion.textContent = navigator.onLine ? 'Navegador conectado' : 'Sin conexión de red';
}
// [Actividad 6] Escucha recuperaciones de conectividad de red del dispositivo.
window.addEventListener('online', actualizarConexion);
// [Actividad 6] Escucha desconexiones de red para advertirlo inmediatamente en la PWA.
window.addEventListener('offline', actualizarConexion);
// [Actividad 6] Evalúa el estado inicial de conectividad al cargar el script.
actualizarConexion();

// [Actividad 6] Inicializa una primera clave `Idempotency-Key` UUID v4 al arrancar la interfaz.
asignarNuevoUuid();

// [Actividad 6] [Actividad 12 - Mejora Visual AutoUTA v5] Registra el Service Worker `/sw.js` (v5) en contextos seguros (HTTPS o localhost) para habilitar instalación PWA.
if ('serviceWorker' in navigator && window.isSecureContext) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}
