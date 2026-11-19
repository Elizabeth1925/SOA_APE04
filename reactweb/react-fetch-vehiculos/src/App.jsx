import { useState, useMemo } from 'react';
import { listarVehiculos } from './api/vehiculosApi.js';
import { listarVentas } from './api/ventasApi.js';
import './App.css';

const dinero = new Intl.NumberFormat('es-EC', {
  style: 'currency',
  currency: 'USD',
});

function mostrarPrecio(precio) {
  if (precio === null || precio === undefined || precio === '') return '—';
  const numero = Number(precio);
  return Number.isFinite(numero) ? dinero.format(numero) : String(precio);
}

function formatearFecha(fechaStr) {
  if (!fechaStr) return '—';
  try {
    const f = new Date(fechaStr);
    return isNaN(f.getTime()) ? String(fechaStr) : f.toLocaleString('es-EC');
  } catch {
    return String(fechaStr);
  }
}

export default function App() {
  const [token, setToken] = useState('');
  const [mostrarToken, setMostrarToken] = useState(false);
  const [pestanaActiva, setPestanaActiva] = useState('vehiculos'); // 'vehiculos' | 'ventas'

  // Estados del catálogo de vehículos
  const [vehiculos, setVehiculos] = useState([]);
  const [marca, setMarca] = useState('');
  const [vehiculoSeleccionado, setVehiculoSeleccionado] = useState(null);

  // Estados de la consulta de ventas
  const [ventas, setVentas] = useState([]);
  const [ventaSeleccionada, setVentaSeleccionada] = useState(null);

  // Estados globales de control HTTP
  const [cargando, setCargando] = useState(false);
  const [mensaje, setMensaje] = useState('Listo para consultar a través de NGINX.');
  const [error, setError] = useState('');
  const [codigoHttp, setCodigoHttp] = useState(null);

  // Filtro de vehículos en memoria por marca
  const vehiculosVisibles = useMemo(() => {
    return vehiculos.filter((v) =>
      String(v.marca ?? '').toLowerCase().includes(marca.trim().toLowerCase()),
    );
  }, [vehiculos, marca]);

  // Métricas calculadas sobre la lista real de ventas recibida
  const metricasVentas = useMemo(() => {
    const totalVentas = ventas.length;
    let montoTotal = 0;
    let exitosas = 0;
    let observadas = 0;

    for (const v of ventas) {
      const precioNum = Number(v.precio ?? v.monto ?? 0);
      if (Number.isFinite(precioNum)) {
        montoTotal += precioNum;
      }
      const est = String(v.estado ?? '').toUpperCase();
      if (est === 'EXITOSA' || est === 'COMPLETADA' || est === 'PROCESADA' || est === 'CONFIRMADA') {
        exitosas++;
      } else {
        observadas++;
      }
    }

    return {
      totalVentas,
      montoTotal,
      exitosas,
      observadas,
    };
  }, [ventas]);

  // Consulta de Vehículos (GET /api/vehiculos vía NGINX 8090)
  async function consultarVehiculos(evento) {
    if (evento) evento.preventDefault();
    setCargando(true);
    setError('');
    setMensaje('Consultando catálogo de vehículos a través del Gateway NGINX...');
    setCodigoHttp(null);
    setVehiculos([]);
    setVehiculoSeleccionado(null);

    try {
      const resultado = await listarVehiculos(token);
      setVehiculos(resultado.vehiculos);
      setCodigoHttp(resultado.codigoHttp);
      setMensaje(`Catálogo consultado con éxito: ${resultado.vehiculos.length} vehículos recibidos.`);
    } catch (e) {
      setCodigoHttp(e.status ?? null);
      setError(e.message);
      setMensaje('La consulta de vehículos no pudo completarse.');
    } finally {
      setCargando(false);
    }
  }

  // Consulta de Ventas (GET /api/ventas vía NGINX 8090)
  async function consultarVentas(evento) {
    if (evento) evento.preventDefault();
    setCargando(true);
    setError('');
    setMensaje('Consultando historial de ventas a través del Gateway NGINX...');
    setCodigoHttp(null);
    setVentas([]);
    setVentaSeleccionada(null);

    try {
      const resultado = await listarVentas(token);
      setVentas(resultado.ventas);
      setCodigoHttp(resultado.codigoHttp);
      setMensaje(`Registro de ventas obtenido: ${resultado.ventas.length} transacciones auditadas.`);
    } catch (e) {
      setCodigoHttp(e.status ?? null);
      setError(e.message);
      setMensaje('La consulta de ventas no pudo completarse.');
    } finally {
      setCargando(false);
    }
  }

  return (
    <main className="contenedor">
      <header className="cabecera">
        <div className="marca-producto">UNIVERSIDAD TÉCNICA DE AMBATO · FISEI · SOA APE 04</div>
        <h1>Portal Web de Consulta y Auditoría</h1>
        <p>Cliente React para supervisión de vehículos y ventas a través del Gateway NGINX (Puerto 8090).</p>
      </header>

      {/* PANEL 1: AUTENTICACIÓN Y CONEXIÓN */}
      <section className="panel" aria-labelledby="titulo-acceso">
        <h2 id="titulo-acceso">1. Conexión y Seguridad JWT</h2>
        <p className="ayuda">
          Las peticiones son enrutadas por Vite hacia <code>http://127.0.0.1:8090</code> (NGINX), el cual balancea hacia Spring Boot y reenvía hacia FastAPI.
        </p>

        <form onSubmit={pestanaActiva === 'vehiculos' ? consultarVehiculos : consultarVentas}>
          <label htmlFor="jwt">Token JWT de Autorización (sin prefijo Bearer)</label>
          <div className="linea-token">
            <input
              id="jwt"
              type={mostrarToken ? 'text' : 'password'}
              autoComplete="off"
              spellCheck="false"
              placeholder="Pegar token JWT obtenido por OAuth 2.0"
              value={token}
              onChange={(e) => setToken(e.target.value)}
            />
            <button
              className="secundario"
              type="button"
              onClick={() => setMostrarToken((actual) => !actual)}
              aria-label={mostrarToken ? 'Ocultar token' : 'Mostrar token'}
            >
              {mostrarToken ? 'Ocultar' : 'Mostrar'}
            </button>
          </div>

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

        <div className={error ? 'estado estado-error' : 'estado estado-ok'} role="status" aria-live="polite">
          <span>{error || mensaje}</span>
          {codigoHttp !== null && <strong>HTTP {codigoHttp}</strong>}
        </div>
        <p className="nota">Seguridad de credenciales: El JWT permanece únicamente en memoria del navegador durante la sesión activa.</p>
      </section>

      {/* BARRA DE NAVEGACIÓN DE PESTAÑAS */}
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

      {/* PESTAÑA 1: CATÁLOGO DE VEHÍCULOS */}
      {pestanaActiva === 'vehiculos' && (
        <>
          <section className="panel" aria-labelledby="titulo-listado">
            <div className="titulo-listado">
              <div>
                <h2 id="titulo-listado">2. Catálogo de Vehículos</h2>
                <p className="ayuda">Servicio de Vehículos balanceado con Round Robin Ponderado (3:1) en NGINX.</p>
              </div>
              <span className="contador">{vehiculosVisibles.length} visibles</span>
            </div>

            <label htmlFor="filtroMarca">Filtrar por marca en tiempo real</label>
            <input
              id="filtroMarca"
              type="search"
              placeholder="Ejemplo: Toyota, Chevrolet, Hyundai..."
              value={marca}
              onChange={(e) => setMarca(e.target.value)}
              disabled={cargando}
            />

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
                  {vehiculosVisibles.map((v, indice) => (
                    <tr key={v.idVehiculo ?? v.id ?? indice}>
                      <td>
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
                        <span className={`badge-estado ${(v.estado || '').toLowerCase()}`}>
                          {v.estado ?? '—'}
                        </span>
                      </td>
                    </tr>
                  ))}
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

          <section className="panel" aria-labelledby="titulo-detalle">
            <h2 id="titulo-detalle">3. Detalle del Vehículo Seleccionado</h2>
            {vehiculoSeleccionado ? (
              <dl className="detalle">
                <div><dt>ID Vehículo</dt><dd>{vehiculoSeleccionado.idVehiculo ?? vehiculoSeleccionado.id ?? '—'}</dd></div>
                <div><dt>Marca</dt><dd>{vehiculoSeleccionado.marca ?? '—'}</dd></div>
                <div><dt>Modelo</dt><dd>{vehiculoSeleccionado.modelo ?? '—'}</dd></div>
                <div><dt>Placa</dt><dd>{vehiculoSeleccionado.placa ?? '—'}</dd></div>
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

      {/* PESTAÑA 2: MÉTRICAS Y AUDITORÍA DE VENTAS */}
      {pestanaActiva === 'ventas' && (
        <>
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

          {/* TARJETAS DE MÉTRICAS */}
          <div className="metricas-grilla">
            <div className="metrica-card">
              <div className="metrica-titulo">Total de Ventas</div>
              <div className="metrica-valor">{metricasVentas.totalVentas}</div>
              <div className="metrica-sub">Transacciones registradas</div>
            </div>
            <div className="metrica-card metrica-dinero">
              <div className="metrica-titulo">Monto Facturado</div>
              <div className="metrica-valor">{dinero.format(metricasVentas.montoTotal)}</div>
              <div className="metrica-sub">Volumen económico transaccionado</div>
            </div>
            <div className="metrica-card metrica-exito">
              <div className="metrica-titulo">Ventas Exitosas</div>
              <div className="metrica-valor">{metricasVentas.exitosas}</div>
              <div className="metrica-sub">Confirmadas y notificadas</div>
            </div>
            <div className="metrica-card metrica-observada">
              <div className="metrica-titulo">Ventas en Revisión</div>
              <div className="metrica-valor">{metricasVentas.observadas}</div>
              <div className="metrica-sub">Pendientes o estados especiales</div>
            </div>
          </div>

          <section className="panel" aria-labelledby="titulo-tabla-ventas">
            <div className="titulo-listado">
              <div>
                <h2 id="titulo-tabla-ventas">Registro Detallado de Ventas</h2>
                <p className="ayuda">Datos transaccionales persistidos en MySQL a través de FastAPI y NGINX.</p>
              </div>
              <div className="acciones-inline">
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
                  {ventas.map((v, indice) => (
                    <tr key={v.id ?? v.idVenta ?? indice}>
                      <td>
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

          <section className="panel" aria-labelledby="titulo-detalle-venta">
            <h2 id="titulo-detalle-venta">Detalle de la Venta Auditada</h2>
            {ventaSeleccionada ? (
              <dl className="detalle">
                <div><dt>ID Venta</dt><dd>#{ventaSeleccionada.id ?? ventaSeleccionada.idVenta ?? '—'}</dd></div>
                <div><dt>ID Vehículo</dt><dd>{ventaSeleccionada.vehiculo_id ?? ventaSeleccionada.vehiculoId ?? '—'}</dd></div>
                <div><dt>ID Comprador</dt><dd>{ventaSeleccionada.comprador_id ?? ventaSeleccionada.compradorId ?? '—'}</dd></div>
                <div><dt>Precio Transaccionado</dt><dd>{mostrarPrecio(ventaSeleccionada.precio ?? ventaSeleccionada.monto)}</dd></div>
                <div><dt>Estado</dt><dd>{ventaSeleccionada.estado ?? '—'}</dd></div>
                <div><dt>Fecha de Registro</dt><dd>{formatearFecha(ventaSeleccionada.fecha ?? ventaSeleccionada.created_at)}</dd></div>
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
