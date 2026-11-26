/*
 * ============================================================================
 * Archivo: src/App.jsx
 * Proyecto: Portal Web React de Consulta y Auditoría (react-fetch-vehiculos) - SOA APE 04
 * Función:
 *   Componente principal del portal web de consulta y auditoría ejecutado en
 *   el puerto dedicado 5174 (proxy Vite hacia Gateway NGINX :8090).
 *   Provee dos vistas estrictamente de lectura:
 *   1) Catálogo de Vehículos: consulta autenticada (GET /api/vehiculos),
 *      filtrado en tiempo real y remoto por marca, manejo explícito de listado
 *      vacío y ficha técnica detallada (incluyendo placa y chasis/VIN).
 *   2) Métricas y Auditoría de Ventas: consulta autenticada (GET /api/ventas),
 *      cálculo de KPIs reales en memoria, tabla de auditoría y vista detallada
 *      por transacción, prohibiendo explícitamente POST /api/ventas y cualquier
 *      formulario de registro de ventas (exclusivo de la PWA móvil).
 * Actividades APE 04 cubiertas:
 *   - [Actividad 7]: Portal web en :5174 con proxy a NGINX (:8090), consulta de
 *     vehículos, filtro por marca, ficha técnica (VIN/placa), listado vacío,
 *     pestaña Métricas y Auditoría de Ventas con KPIs reales y detalle
 *     individual, sin POST /api/ventas ni formularios de creación de ventas.
 *   - [Actividad 10 - P27]: Coherencia transversal del catálogo reflejando el
 *     cambio de estado DISPONIBLE -> VENDIDO y visualización de ventas reales
 *     emitidas desde la PWA.
 *   - [Actividad 11]: Manejo controlado de HTTP 401 (sin token o token inválido/
 *     vencido) y errores de gateway, limpiando el estado sin inventar datos.
 * ============================================================================
 */
// Importa hooks de React: useState para estado reactivo y useMemo para cálculos derivados (filtro y KPIs).
import { useState, useMemo } from 'react';
// Importa el cliente HTTP de solo lectura para GET /api/vehiculos vía NGINX [Actividad 7].
import { listarVehiculos } from './api/vehiculosApi.js';
// Importa el cliente HTTP de solo lectura para GET /api/ventas vía NGINX (sin POST) [Actividad 7].
import { listarVentas } from './api/ventasApi.js';
// Importa los estilos visuales específicos del portal web de consulta y auditoría.
import './App.css';

// Formateador internacional de moneda configurado para dólares estadounidenses en Ecuador (es-EC).
const dinero = new Intl.NumberFormat('es-EC', {
  // Indica formato monetario con símbolo de divisa.
  style: 'currency',
  // Divisa USD utilizada en los precios del catálogo y montos facturados.
  currency: 'USD',
});

// Formatea de forma segura un precio numérico a USD o muestra "—" cuando el dato está ausente [Actividad 7].
function mostrarPrecio(precio) {
  // Si el precio es nulo, indefinido o cadena vacía, retorna guion largo como marcador visual.
  if (precio === null || precio === undefined || precio === '') return '—';
  // Convierte el valor a número de punto flotante para validar si es formateable.
  const numero = Number(precio);
  // Si es un número finito lo formatea como moneda USD; en caso contrario devuelve el texto original.
  return Number.isFinite(numero) ? dinero.format(numero) : String(precio);
}

// Convierte una fecha ISO/UTC proveniente del backend en fecha/hora legible local (es-EC) [Actividad 7].
function formatearFecha(fechaStr) {
  // Si no hay valor de fecha, devuelve un guion largo.
  if (!fechaStr) return '—';
  try {
    // Intenta parsear la cadena a objeto Date de JavaScript.
    const f = new Date(fechaStr);
    // Si la fecha es inválida conserva la cadena original; si es válida la formatea en locale es-EC.
    return isNaN(f.getTime()) ? String(fechaStr) : f.toLocaleString('es-EC');
  } catch {
    // Ante cualquier excepción inesperada retorna la representación textual original sin fallar.
    return String(fechaStr);
  }
}

// Declara y exporta el componente funcional raíz App del Portal Web de Consulta y Auditoría [Actividad 7].
export default function App() {
  // Estado del token JWT almacenado exclusivamente en memoria del navegador (nunca persistido en disco) [Actividad 7 / Actividad 11].
  const [token, setToken] = useState('');
  // Controla si el campo del token JWT se muestra en texto claro o enmascarado tipo password.
  const [mostrarToken, setMostrarToken] = useState(false);
  // Controla la pestaña activa del portal: 'vehiculos' (Catálogo) o 'ventas' (Métricas y Auditoría) [Actividad 7].
  const [pestanaActiva, setPestanaActiva] = useState('vehiculos'); // 'vehiculos' | 'ventas'

  // Estados del catálogo de vehículos [Actividad 7 / Actividad 10 - P27].
  // Lista real de vehículos recibida desde GET /api/vehiculos.
  const [vehiculos, setVehiculos] = useState([]);
  // Texto ingresado por el usuario para filtrar el catálogo por marca (local y remoto).
  const [marca, setMarca] = useState('');
  // Registro de vehículo seleccionado al hacer clic en su ID para mostrar su ficha técnica (VIN/placa) [Actividad 7].
  const [vehiculoSeleccionado, setVehiculoSeleccionado] = useState(null);

  // Estados de la consulta de ventas (solo lectura / auditoría) [Actividad 7 / Actividad 10 - P27].
  // Lista real de transacciones recibida desde GET /api/ventas.
  const [ventas, setVentas] = useState([]);
  // Registro de venta seleccionado para inspeccionar sus atributos completos (UUID, comprador, notificación) [Actividad 7].
  const [ventaSeleccionada, setVentaSeleccionada] = useState(null);

  // Estados globales de control HTTP y retroalimentación controlada [Actividad 11].
  // Indica si existe una solicitud HTTP en vuelo para deshabilitar botones y evitar duplicados.
  const [cargando, setCargando] = useState(false);
  // Mensaje informativo mostrado en el banner de estado cuando no hay error.
  const [mensaje, setMensaje] = useState('Listo para consultar a través de NGINX.');
  // Mensaje de error controlado (ej. HTTP 401 sin token o token vencido, 403, 502/504) [Actividad 11].
  const [error, setError] = useState('');
  // Último código de estado HTTP real retornado por NGINX/backend (200, 204, 401, 403, 502, etc.) [Actividad 11].
  const [codigoHttp, setCodigoHttp] = useState(null);

  // Filtro de vehículos en memoria por marca (case-insensitive) memoizado para eficiencia [Actividad 7].
  const vehiculosVisibles = useMemo(() => {
    // Filtra la lista real conservando aquellos cuya marca contenga el texto buscado.
    return vehiculos.filter((v) =>
      String(v.marca ?? '').toLowerCase().includes(marca.trim().toLowerCase()),
    );
  }, [vehiculos, marca]);

  // Métricas (KPIs) calculadas estrictamente sobre la lista real de ventas recibida de GET /api/ventas [Actividad 7 / Actividad 10 - P27].
  const metricasVentas = useMemo(() => {
    // Conteo total de ventas reales cargadas.
    const totalVentas = ventas.length;
    // Acumulador del volumen económico facturado correspondiente a ventas exitosas.
    let montoTotal = 0;
    // Contador de ventas confirmadas/exitosas/procesadas.
    let exitosas = 0;
    // Contador de ventas en estados especiales, pendientes o en revisión.
    let observadas = 0;

    // Recorre cada venta real para clasificar su estado y sumar su precio acordado [Actividad 7].
    for (const v of ventas) {
      // Normaliza el campo estado a mayúsculas tolerando valores nulos.
      const est = String(v.estado ?? '').toUpperCase();
      // Determina si la transacción corresponde a un estado de venta exitosa.
      const esExitosa = est === 'EXITOSA' || est === 'COMPLETADA' || est === 'PROCESADA' || est === 'CONFIRMADA';
      // Si es exitosa incrementa el contador y acumula su monto real.
      if (esExitosa) {
        exitosas++;
        // Lee el monto tolerando variantes del esquema JSON (precio, precioAcordado o monto).
        const precioNum = Number(v.precio ?? v.precioAcordado ?? v.monto ?? 0);
        // Suma solo valores numéricos finitos para evitar NaN en el KPI de facturación.
        if (Number.isFinite(precioNum)) {
          montoTotal += precioNum;
        }
      } else {
        // Clasifica cualquier otro estado como venta observada o en revisión.
        observadas++;
      }
    }

    // Devuelve el objeto de KPIs reales listo para renderizarse en las tarjetas de métricas [Actividad 7].
    return {
      totalVentas,
      montoTotal,
      exitosas,
      observadas,
    };
  }, [ventas]);

  // Consulta de Vehículos (GET /api/vehiculos o GET /api/vehiculos?marca=... vía NGINX :8090) [Actividad 7 / Actividad 10 - P27 / Actividad 11].
  async function consultarVehiculos(evento, filtroServidor = '') {
    // Evita la recarga de página cuando se dispara mediante el envío del formulario HTML.
    if (evento) evento.preventDefault();
    // Activa el indicador de carga y limpia cualquier error HTTP previo.
    setCargando(true);
    setError('');
    // Extrae y recorta el filtro remoto de marca si se proporcionó como texto válido [Actividad 7].
    const marcaQuery = typeof filtroServidor === 'string' ? filtroServidor.trim() : '';
    // Actualiza el mensaje de progreso indicando si la consulta lleva filtro de marca en servidor.
    setMensaje(
      marcaQuery
        ? `Consultando vehículos de marca "${marcaQuery}" a través del Gateway NGINX...`
        : 'Consultando catálogo de vehículos a través del Gateway NGINX...',
    );
    // Limpia el código HTTP, la lista anterior y la ficha seleccionada antes de consultar para no mostrar datos obsoletos ni inventados [Actividad 11].
    setCodigoHttp(null);
    setVehiculos([]);
    setVehiculoSeleccionado(null);

    try {
      // Ejecuta GET /api/vehiculos con el JWT actual y el filtro opcional de marca [Actividad 7].
      const resultado = await listarVehiculos(token, marcaQuery);
      // Carga en estado la lista real devuelta por el microservicio (reflejando DISPONIBLE -> VENDIDO) [Actividad 10 - P27].
      setVehiculos(resultado.vehiculos);
      // Registra el código HTTP real exitoso (200 o 204).
      setCodigoHttp(resultado.codigoHttp);
      // Informa el conteo exacto de vehículos recibidos.
      setMensaje(
        marcaQuery
          ? `Filtro remoto aplicado (${marcaQuery}): ${resultado.vehiculos.length} vehículos recibidos.`
          : `Catálogo consultado con éxito: ${resultado.vehiculos.length} vehículos recibidos.`,
      );
    } catch (e) {
      // Manejo controlado de errores (ej. HTTP 401 sin token o vencido, 403, 502/504): preserva estado vacío sin inventar registros [Actividad 11].
      setCodigoHttp(e.status ?? null);
      setError(e.message);
      setMensaje('La consulta de vehículos no pudo completarse.');
    } finally {
      // Desactiva el indicador de carga al finalizar tanto en éxito como en error.
      setCargando(false);
    }
  }

  // Consulta de Ventas en modo exclusivo de auditoría (GET /api/ventas vía NGINX :8090, nunca POST) [Actividad 7 / Actividad 10 - P27 / Actividad 11].
  async function consultarVentas(evento) {
    // Evita la recarga del navegador si se dispara al presionar Enter en el formulario.
    if (evento) evento.preventDefault();
    // Activa el estado de carga y limpia alertas anteriores.
    setCargando(true);
    setError('');
    setMensaje('Consultando historial de ventas a través del Gateway NGINX...');
    // Reinicia código HTTP, arreglo de ventas y detalle seleccionado para evitar exhibir datos viejos ante un 401 [Actividad 11].
    setCodigoHttp(null);
    setVentas([]);
    setVentaSeleccionada(null);

    try {
      // Ejecuta GET /api/ventas con el JWT actual a través de ventasApi.js [Actividad 7].
      const resultado = await listarVentas(token);
      // Guarda las ventas reales devueltas por FastAPI/MySQL para recalcular KPIs y tabla [Actividad 10 - P27].
      setVentas(resultado.ventas);
      // Registra el código HTTP real (200 o 204).
      setCodigoHttp(resultado.codigoHttp);
      // Informa cuántas transacciones reales fueron auditadas.
      setMensaje(`Registro de ventas obtenido: ${resultado.ventas.length} transacciones auditadas.`);
    } catch (e) {
      // Manejo controlado de HTTP 401/403/502/504 o fallo de red sin fabricar ventas ficticias [Actividad 11].
      setCodigoHttp(e.status ?? null);
      setError(e.message);
      setMensaje('La consulta de ventas no pudo completarse.');
    } finally {
      // Libera el bloqueo de controles al concluir la petición.
      setCargando(false);
    }
  }

  // Retorna el árbol JSX completo del Portal Web de Consulta y Auditoría [Actividad 7].
  return (
    <main className="contenedor">
      {/* Cabecera institucional del portal web cliente React conectado vía Gateway NGINX (:8090) [Actividad 7] */}
      <header className="cabecera">
        <div className="marca-producto">UNIVERSIDAD TÉCNICA DE AMBATO · FISEI · SOA APE 04</div>
        <h1>Portal Web de Consulta y Auditoría</h1>
        <p>Cliente React para supervisión de vehículos y ventas a través del Gateway NGINX (Puerto 8090).</p>
      </header>

      {/* PANEL 1: AUTENTICACIÓN Y CONEXIÓN JWT EN MEMORIA Y BANNER DE ESTADO HTTP [Actividad 7 / Actividad 11] */}
      <section className="panel" aria-labelledby="titulo-acceso">
        <h2 id="titulo-acceso">1. Conexión y Seguridad JWT</h2>
        <p className="ayuda">
          Las peticiones son enrutadas por Vite hacia <code>http://127.0.0.1:8090</code> (NGINX), el cual balancea hacia Spring Boot y reenvía hacia FastAPI.
        </p>

        {/* Formulario que dispara la consulta GET correspondiente a la pestaña activa (nunca POST /api/ventas) [Actividad 7] */}
        <form onSubmit={pestanaActiva === 'vehiculos' ? consultarVehiculos : consultarVentas}>
          <label htmlFor="jwt">Token JWT de Autorización (sin prefijo Bearer)</label>
          <div className="linea-token">
            {/* Entrada controlada del JWT con opción de ocultar/mostrar; dejarla vacía permite verificar HTTP 401 [Actividad 11] */}
            <input
              id="jwt"
              type={mostrarToken ? 'text' : 'password'}
              autoComplete="off"
              spellCheck="false"
              placeholder="Pegar token JWT obtenido por OAuth 2.0"
              value={token}
              onChange={(e) => setToken(e.target.value)}
            />
            {/* Botón accesible para alternar la visibilidad del token ingresado */}
            <button
              className="secundario"
              type="button"
              onClick={() => setMostrarToken((actual) => !actual)}
              aria-label={mostrarToken ? 'Ocultar token' : 'Mostrar token'}
            >
              {mostrarToken ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>

          {/* Botones de ejecución de consulta (Vehículos o Ventas) y limpieza inmediata del token */}
          <div className="acciones">
            <button
              type="button"
              disabled={cargando}
              onClick={pestanaActiva === 'vehiculos' ? consultarVehiculos : consultarVentas}
            >
              {cargando ? 'Consultando...' : pestanaActiva === 'vehiculos' ? 'Consultar Vehículos' : 'Consultar Ventas'}
            </button>
            <button
              className="secundario"
              type="button"
              disabled={cargando}
              onClick={() => {
                setToken('');
                setMostrarToken(false);
              }}
            >
              Limpiar token
            </button>
          </div>
        </form>

        {/* Banner accesible de estado HTTP: muestra éxito o error controlado (401/403/502/504) junto al código real [Actividad 11] */}
        <div className={error ? 'estado estado-error' : 'estado estado-ok'} role="status" aria-live="polite">
          <span>{error || mensaje}</span>
          {codigoHttp !== null && <strong>HTTP {codigoHttp}</strong>}
        </div>
        <p className="nota">Seguridad de credenciales: El JWT permanece únicamente en memoria del navegador durante la sesión activa.</p>
      </section>

      {/* BARRA DE NAVEGACIÓN DE PESTAÑAS: separa Catálogo de Vehículos y Métricas/Auditoría de Ventas [Actividad 7] */}
      <div className="pestanas-nav">
        <button
          className={pestanaActiva === 'vehiculos' ? 'pestana-btn activa' : 'pestana-btn'}
          type="button"
          onClick={() => setPestanaActiva('vehiculos')}
        >
          🚗 Catálogo de Vehículos
        </button>
        <button
          className={pestanaActiva === 'ventas' ? 'pestana-btn activa' : 'pestana-btn'}
          type="button"
          onClick={() => setPestanaActiva('ventas')}
        >
          📊 Métricas y Auditoría de Ventas
        </button>
      </div>

      {/* PESTAÑA 1: CATÁLOGO DE VEHÍCULOS (GET /api/vehiculos, filtro por marca, tabla y ficha técnica VIN/placa) [Actividad 7 / Actividad 10 - P27] */}
      {pestanaActiva === 'vehiculos' && (
        <>
          {/* Panel del listado de vehículos y barra de filtrado por marca [Actividad 7] */}
          <section className="panel" aria-labelledby="titulo-listado">
            <div className="titulo-listado">
              <div>
                <h2 id="titulo-listado">2. Catálogo de Vehículos</h2>
                <p className="ayuda">Servicio de Vehículos balanceado con Round Robin Ponderado (3:1) en NGINX.</p>
              </div>
              {/* Contador en tiempo real de vehículos que coinciden con el filtro activo */}
              <span className="contador">{vehiculosVisibles.length} visibles</span>
            </div>

            {/* Control de filtrado por marca: filtra instantáneamente en memoria y permite consulta directa ?marca= en API [Actividad 7] */}
            <label htmlFor="filtroMarca">Filtrar por marca (en tiempo real o consulta directa en API)</label>
            <div className="linea-token">
              <input
                id="filtroMarca"
                type="search"
                placeholder="Ejemplo: Toyota, Chevrolet, Hyundai..."
                value={marca}
                onChange={(e) => setMarca(e.target.value)}
                disabled={cargando}
              />
              {/* Dispara GET /api/vehiculos?marca=... contra el backend vía NGINX [Actividad 7] */}
              <button
                className="secundario"
                type="button"
                disabled={cargando || !marca.trim()}
                onClick={() => consultarVehiculos(null, marca)}
              >
                Filtrar en API
              </button>
              {/* Botón condicional para limpiar el criterio de marca y recargar el catálogo completo */}
              {marca.trim() && (
                <button
                  className="secundario"
                  type="button"
                  disabled={cargando}
                  onClick={() => {
                    setMarca('');
                    consultarVehiculos(null, '');
                  }}
                >
                  Limpiar
                </button>
              )}
            </div>

            {/* Tabla responsiva del catálogo con estados DISPONIBLE/VENDIDO y selección de detalle por ID [Actividad 7 / Actividad 10 - P27] */}
            <div className="tabla-scroll">
              <table>
                <thead>
                  <tr>
                    <th scope="col">ID</th>
                    <th scope="col">Marca</th>
                    <th scope="col">Modelo</th>
                    <th scope="col">Año</th>
                    <th scope="col">Color</th>
                    <th scope="col">Precio</th>
                    <th scope="col">Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {/* Renderiza cada vehículo visible tolerando variaciones de clave (idVehiculo/id) */}
                  {vehiculosVisibles.map((v, indice) => (
                    <tr key={v.idVehiculo ?? v.id ?? indice}>
                      <td>
                        {/* Botón enlace que selecciona el vehículo para mostrar su ficha técnica completa (placa y VIN) [Actividad 7] */}
                        <button
                          className="enlace-id"
                          type="button"
                          onClick={() => setVehiculoSeleccionado(v)}
                          aria-label={`Ver detalle del vehículo ${v.idVehiculo ?? v.id ?? ''}`}
                        >
                          {v.idVehiculo ?? v.id ?? '—'}
                        </button>
                      </td>
                      <td>{v.marca ?? '—'}</td>
                      <td>{v.modelo ?? '—'}</td>
                      <td>{v.anio ?? '—'}</td>
                      <td>{v.color ?? '—'}</td>
                      <td>{mostrarPrecio(v.precio)}</td>
                      <td>
                        {/* Insignia visual que refleja coherencia transversal DISPONIBLE -> VENDIDO tras compras en PWA [Actividad 10 - P27] */}
                        <span className={`badge-estado ${(v.estado || '').toLowerCase()}`}>
                          {v.estado ?? '—'}
                        </span>
                      </td>
                    </tr>
                  ))}
                  {/* Manejo explícito de listado vacío (en carga, sin consulta inicial o sin coincidencias de marca) [Actividad 7] */}
                  {vehiculosVisibles.length === 0 && (
                    <tr>
                      <td className="sin-datos" colSpan="7">
                        {cargando
                          ? 'Cargando vehículos desde NGINX...'
                          : vehiculos.length === 0
                          ? 'Aún no hay vehículos cargados. Presione "Consultar Vehículos".'
                          : 'No se encontraron vehículos que coincidan con la marca ingresada.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Panel de ficha técnica detallada del vehículo seleccionado, incluyendo Placa y Chasis (VIN) [Actividad 7] */}
          <section className="panel" aria-labelledby="titulo-detalle">
            <h2 id="titulo-detalle">3. Detalle del Vehículo Seleccionado</h2>
            {vehiculoSeleccionado ? (
              <dl className="detalle">
                <div><dt>ID Vehículo</dt><dd>{vehiculoSeleccionado.idVehiculo ?? vehiculoSeleccionado.id ?? '—'}</dd></div>
                <div><dt>Marca</dt><dd>{vehiculoSeleccionado.marca ?? '—'}</dd></div>
                <div><dt>Modelo</dt><dd>{vehiculoSeleccionado.modelo ?? '—'}</dd></div>
                {/* Placa y Chasis (VIN) requeridos en la ficha técnica detallada [Actividad 7] */}
                <div><dt>Placa</dt><dd>{vehiculoSeleccionado.placa ?? '—'}</dd></div>
                <div><dt>Chasis (VIN)</dt><dd>{vehiculoSeleccionado.chasis ?? '—'}</dd></div>
                <div><dt>Año</dt><dd>{vehiculoSeleccionado.anio ?? '—'}</dd></div>
                <div><dt>Color</dt><dd>{vehiculoSeleccionado.color ?? '—'}</dd></div>
                <div><dt>Precio</dt><dd>{mostrarPrecio(vehiculoSeleccionado.precio)}</dd></div>
                <div><dt>Estado</dt><dd>{vehiculoSeleccionado.estado ?? '—'}</dd></div>
              </dl>
            ) : (
              <p className="ayuda">Haga clic en el ID de un vehículo de la tabla para visualizar sus detalles completos.</p>
            )}
          </section>
        </>
      )}

      {/* PESTAÑA 2: MÉTRICAS Y AUDITORÍA DE VENTAS (SOLO LECTURA GET /api/ventas; PROHIBIDO POST /api/ventas) [Actividad 7 / Actividad 10 - P27] */}
      {pestanaActiva === 'ventas' && (
        <>
          {/* Aviso explícito de aislamiento de perfil: confirma ausencia de formularios de registro de venta [Actividad 7] */}
          <section className="panel panel-aviso-seguridad">
            <div className="aviso-cabecera">
              <span className="aviso-icono">ℹ️</span>
              <div>
                <strong>Aislamiento de Perfil y Canal de Auditoría</strong>
                <p className="aviso-texto">
                  Este cliente web opera en modo exclusivo de <strong>supervisión y consulta</strong> (<code>GET /api/ventas</code>).
                  De acuerdo con las reglas de seguridad de la arquitectura SOA multicanal, el registro y emisión transaccional
                  de ventas pertenece con exclusividad a la Aplicación Móvil PWA.
                </p>
              </div>
            </div>
          </section>

          {/* TARJETAS DE MÉTRICAS (KPIs calculados en tiempo real exclusivamente sobre ventas reales) [Actividad 7 / Actividad 10 - P27] */}
          <div className="metricas-grilla">
            {/* KPI 1: Conteo total de transacciones registradas */}
            <div className="metrica-card">
              <div className="metrica-titulo">Total de Ventas</div>
              <div className="metrica-valor">{metricasVentas.totalVentas}</div>
              <div className="metrica-sub">Transacciones registradas</div>
            </div>
            {/* KPI 2: Sumatoria monetaria en USD de las ventas exitosas */}
            <div className="metrica-card metrica-dinero">
              <div className="metrica-titulo">Monto Facturado</div>
              <div className="metrica-valor">{dinero.format(metricasVentas.montoTotal)}</div>
              <div className="metrica-sub">Volumen económico transaccionado</div>
            </div>
            {/* KPI 3: Conteo de ventas confirmadas/procesadas */}
            <div className="metrica-card metrica-exito">
              <div className="metrica-titulo">Ventas Exitosas</div>
              <div className="metrica-valor">{metricasVentas.exitosas}</div>
              <div className="metrica-sub">Confirmadas y notificadas</div>
            </div>
            {/* KPI 4: Conteo de ventas observadas o pendientes */}
            <div className="metrica-card metrica-observada">
              <div className="metrica-titulo">Ventas en Revisión</div>
              <div className="metrica-valor">{metricasVentas.observadas}</div>
              <div className="metrica-sub">Pendientes o estados especiales</div>
            </div>
          </div>

          {/* Tabla de auditoría detallada de transacciones reales obtenidas de GET /api/ventas [Actividad 7 / Actividad 10 - P27] */}
          <section className="panel" aria-labelledby="titulo-tabla-ventas">
            <div className="titulo-listado">
              <div>
                <h2 id="titulo-tabla-ventas">Registro Detallado de Ventas</h2>
                <p className="ayuda">Datos transaccionales persistidos en MySQL a través de FastAPI y NGINX.</p>
              </div>
              <div className="acciones-inline">
                {/* Botón de refresco manual para sincronizar ventas recién registradas en la PWA [Actividad 10 - P27] */}
                <button
                  type="button"
                  className="boton-recargar"
                  disabled={cargando}
                  onClick={consultarVentas}
                >
                  ↻ Actualizar Ventas
                </button>
                <span className="contador">{ventas.length} ventas</span>
              </div>
            </div>

            <div className="tabla-scroll">
              <table>
                <thead>
                  <tr>
                    <th scope="col">ID Venta</th>
                    <th scope="col">ID Vehículo</th>
                    <th scope="col">ID Comprador</th>
                    <th scope="col">Precio Venta</th>
                    <th scope="col">Estado</th>
                    <th scope="col">Fecha / Hora</th>
                  </tr>
                </thead>
                <tbody>
                  {/* Renderiza cada venta real tolerando convenciones snake_case de FastAPI y camelCase */}
                  {ventas.map((v, indice) => (
                    <tr key={v.id ?? v.idVenta ?? indice}>
                      <td>
                        {/* Botón enlace para abrir el panel inferior con el detalle individual de auditoría [Actividad 7] */}
                        <button
                          className="enlace-id"
                          type="button"
                          onClick={() => setVentaSeleccionada(v)}
                          aria-label={`Ver detalle de venta ${v.id ?? v.idVenta ?? ''}`}
                        >
                          #{v.id ?? v.idVenta ?? '—'}
                        </button>
                      </td>
                      <td>{v.vehiculo_id ?? v.vehiculoId ?? '—'}</td>
                      <td>{v.comprador_id ?? v.compradorId ?? '—'}</td>
                      <td>{mostrarPrecio(v.precio ?? v.monto)}</td>
                      <td>
                        <span className={`badge-estado ${String(v.estado || '').toLowerCase()}`}>
                          {v.estado ?? 'PROCESADA'}
                        </span>
                      </td>
                      <td>{formatearFecha(v.fecha ?? v.created_at)}</td>
                    </tr>
                  ))}
                  {/* Manejo explícito de estado vacío en la tabla de auditoría de ventas [Actividad 7] */}
                  {ventas.length === 0 && (
                    <tr>
                      <td className="sin-datos" colSpan="6">
                        {cargando
                          ? 'Consultando ventas desde FastAPI / NGINX...'
                          : 'No hay ventas en la vista actual. Presione "Consultar Ventas" para obtener los datos.'}
                      </td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>

          {/* Panel de inspección individual de la venta auditada (UUID de operación, vehículo, comprador, RabbitMQ, fecha UTC) [Actividad 7 / Actividad 10 - P27] */}
          <section className="panel" aria-labelledby="titulo-detalle-venta">
            <h2 id="titulo-detalle-venta">Detalle de la Venta Auditada</h2>
            {ventaSeleccionada ? (
              <dl className="detalle">
                <div><dt>ID Venta</dt><dd>#{ventaSeleccionada.id ?? ventaSeleccionada.idVenta ?? '—'}</dd></div>
                <div><dt>ID Operación (UUID)</dt><dd>{ventaSeleccionada.operacionId ?? ventaSeleccionada.operacion_id ?? '—'}</dd></div>
                <div><dt>ID Vehículo</dt><dd>{ventaSeleccionada.vehiculo_id ?? ventaSeleccionada.vehiculoId ?? '—'}</dd></div>
                <div><dt>ID Comprador</dt><dd>{ventaSeleccionada.comprador_id ?? ventaSeleccionada.compradorId ?? '—'}</dd></div>
                <div><dt>Precio Acordado</dt><dd>{mostrarPrecio(ventaSeleccionada.precio ?? ventaSeleccionada.precioAcordado ?? ventaSeleccionada.monto)} {ventaSeleccionada.moneda ?? ''}</dd></div>
                <div><dt>Estado</dt><dd>{ventaSeleccionada.estado ?? '—'}</dd></div>
                <div><dt>Notificación RabbitMQ</dt><dd>{ventaSeleccionada.notificacion ?? '—'}</dd></div>
                <div><dt>Fecha UTC</dt><dd>{formatearFecha(ventaSeleccionada.fechaUTC ?? ventaSeleccionada.fecha ?? ventaSeleccionada.created_at)}</dd></div>
              </dl>
            ) : (
              <p className="ayuda">Haga clic en el número de ID de una venta para inspeccionar sus atributos completos.</p>
            )}
          </section>
        </>
      )}
    </main>
  );
}
