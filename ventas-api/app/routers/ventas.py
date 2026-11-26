"""
===============================================================================
ARCHIVO: app/routers/ventas.py
MICROSERVICIO: ventas-api (FastAPI, puerto :8002 detras de NGINX :8090)
===============================================================================
RESPONSABILIDAD EN LA ARQUITECTURA SOA:
    Controlador REST principal de `ventas-api`. Expone los 5 endpoints del
    nucleo de Compradores y Ventas bajo el prefijo `/api` (enrutados desde el
    API Gateway NGINX `:8090` hacia FastAPI `:8002`):
      - `POST /api/compradores`
      - `GET  /api/compradores`
      - `POST /api/ventas` (con cabecera obligatoria `Idempotency-Key`)
      - `GET  /api/ventas`
      - `GET  /api/ventas/{id}`
    Integra autenticacion delegada a Java (`Depends(identidad)`), control RBAC
    (`vendedor()`, `propietario()`), cabeceras REST (`Location`) y codigos HTTP
    dinamicos de idempotencia (`201`, `200`, `202`).

ACTIVIDADES APE 04 CUBIERTAS:
    - [Actividad 1 / Actividad 2]: Contratos REST de Compradores y Ventas.
    - [Actividad 4]: Endpoints nucleo de Compradores y Ventas en FastAPI,
      validacion estricta de `Idempotency-Key` (8 a 100 chars alfanumericos),
      guardias RBAC y despacho hacia `venta_service.crear()` con codigos
      dinamicos `201 Created`, `200 OK` (reintento idempotente) o `202 Accepted`.
    - [Actividad 10 - P27]: Devuelve identificadores coherentes (`id`,
      `compradorId`, `vehiculoId`, `operacionId`) consumidos por PWA React,
      Windows Forms y pruebas integrales.
    - [Actividad 11]: Protege contra accesos no autenticados o sin rol
      (`401`/`403`, P10), claves faltantes (`422`), ventas inexistentes (`404`),
      reutilizacion de clave (`409`, P11) y fallos reales de BD (`503`, P13).
===============================================================================
"""

# [Actividad 4] Utilidad `Annotated` para adjuntar metadatos de validacion `Header(...)` al tipo del parametro
from typing import Annotated

# [Actividad 4] Componentes centrales de FastAPI: enrutador, inyeccion de dependencias, cabeceras, respuesta y peticion cruda
from fastapi import APIRouter, Depends, Header, Response, Request

# [Actividad 4] Fabrica de sesiones SQLAlchemy para transacciones cortas de lectura y escritura en MySQL
from app.db import SessionLocal
# [Actividad 4] Modelos ORM `Venta` y `Comprador`
from app.models import Venta, Comprador
# [Actividad 1 / Actividad 2] [Actividad 4] Esquemas Pydantic de validacion de entrada
from app.schemas import CrearVenta, CrearComprador
# [Actividad 4] [Actividad 11] Dependencia de autenticacion delegada a Java y guardias RBAC
from app.security import identidad, vendedor, propietario
# [Actividad 4] [Actividad 11] Excepcion controlada que serializa errores en formato RFC 7807
from app.errors import ApiError
# [Actividad 4] [Actividad 8] Capa de logica de negocio de ventas e integracion con Java y RabbitMQ
from app.services import venta_service as service

# [Actividad 4] Instancia el enrutador REST con el prefijo `/api` esperado por NGINX `:8090`
router = APIRouter(prefix="/api")


# [Actividad 1 / Actividad 2] [Actividad 4] Endpoint de registro de compradores (HTTP 201 Created)
@router.post("/compradores", status_code=201)
def crear_comprador(d: CrearComprador, u=Depends(identidad)):
    """Crea un comprador en MySQL asociado al vendedor autenticado (`ROLE_OPERADOR` o `ROLE_ADMIN`)."""
    # [Actividad 4] [Actividad 11] Exige rol de operador o administrador; rechaza `ROLE_CONSULTA` con HTTP 403 (P10)
    vendedor(u)

    # [Actividad 4] Abre una transaccion atomica corta que hace commit automatico al salir o rollback ante error
    with SessionLocal.begin() as db:
        # Construye la entidad `Comprador` vinculada al `usuario` validado por Spring Boot
        c = Comprador(
            # Usuario propietario del registro para control horizontal de acceso
            usuario=u["usuario"],
            # Documento normalizado por `CrearComprador` (acepta `documento` o `cedula`)
            documento=d.documento,
            # Nombre completo validado
            nombre=d.nombre,
            # Correo electronico normalizado (acepta `correo` o `email`)
            correo=d.correo
        )
        # Registra el nuevo objeto en la sesion SQLAlchemy (si duplica usuario+documento, MySQL lanza `IntegrityError` -> 409)
        db.add(c)
        # Ejecuta `INSERT` inmediato dentro de la transaccion para materializar la clave primaria `c.id`
        db.flush()
        # [Actividad 10 - P27] Retorna el comprador creado incluyendo su `id` (`compradorId`) para enlazarlo en ventas
        return {"id": c.id, "documento": c.documento, "nombre": c.nombre, "correo": c.correo}


# [Actividad 4] Endpoint de consulta de catalogo de compradores para clientes PWA React y Windows Forms
@router.get("/compradores")
def listar_compradores(u=Depends(identidad)):
    """Lista compradores autorizados segun el rol del usuario autenticado via Java."""
    # Abre una sesion corta de solo lectura contra MySQL
    with SessionLocal() as db:
        # [Actividad 4] Los roles operativos y de consulta (`ADMIN`, `OPERADOR`, `CONSULTA`) visualizan el catalogo completo
        if u.get("rol") in ("ADMIN", "OPERADOR", "CONSULTA"):
            # Consulta todos los compradores registrados en MySQL
            compradores = db.query(Comprador).all()
        else:
            # Cualquier otro rol solo puede consultar los compradores que el mismo registro
            compradores = db.query(Comprador).filter(Comprador.usuario == u["usuario"]).all()

        # [Actividad 10 - P27] Serializa la lista de compradores preservando `id`, `usuario`, `documento`, `nombre` y `correo`
        return [
            {
                # Identificador primario (`compradorId`)
                "id": c.id,
                # Propietario que registro al comprador
                "usuario": c.usuario,
                # Documento de identidad
                "documento": c.documento,
                # Nombre o razon social
                "nombre": c.nombre,
                # Correo de contacto
                "correo": c.correo,
            }
            # Itera sobre todos los registros ORM recuperados
            for c in compradores
        ]


# [Actividad 4] [Actividad 8] [Actividad 10 - P27] [Actividad 11] Endpoint principal de creacion idempotente de ventas
@router.post("/ventas")
def crear(d: CrearVenta, response: Response, request: Request,
          # [Actividad 4] Extrae y valida `Idempotency-Key` (8..100 caracteres alfanumericos, guion o guion bajo)
          idempotency_key: Annotated[str | None, Header(
              alias="Idempotency-Key", min_length=8, max_length=100, pattern=r"^[A-Za-z0-9_-]+$")] = None,
          # [Actividad 4] Autenticacion delegada obligatoria via `GET /api/identidad` en Java
          u=Depends(identidad)):
    """Registra o recupera de forma idempotente una venta coordinada con Spring Boot y RabbitMQ."""
    # [Actividad 4] [Actividad 11] Verifica que el usuario autenticado tenga rol `ROLE_OPERADOR` o `ROLE_ADMIN` (P10 -> 403)
    vendedor(u)

    # [Actividad 4] Lectura tolerante de `Idempotency-Key` ante normalizaciones de cabeceras realizadas por proxies (NGINX) o clientes
    key = idempotency_key or request.headers.get("idempotency-key") or request.headers.get("Idempotency-Key") or request.headers.get("idempotency_key")
    # Verifica que finalmente exista una clave valida de al menos 8 caracteres no vacios
    if not key or len(key.strip()) < 8:
        # Lanza HTTP 422 RFC 7807 cuando falta la cabecera obligatoria de idempotencia
        raise ApiError(422, "VALIDACION", "header.idempotency-key: Field required")
    # Elimina espacios laterales accidentales antes de persistir o buscar por clave
    key = key.strip()

    # [Actividad 4] [Actividad 8] [Actividad 11] Delega en el servicio la idempotencia SHA-256, el aislamiento en 3 fases y la emision RabbitMQ
    resultado, codigo = service.crear(d, key, u)

    # [Actividad 4] [Actividad 11] Fija el codigo HTTP real: `201` creada, `200` reintento idempotente (P11) o `202` en revision (P13)
    response.status_code = codigo

    # [Actividad 4] [Actividad 10 - P27] Agrega la cabecera estandar REST `Location` apuntando al recurso `/api/ventas/{id}`
    response.headers["Location"] = f"/api/ventas/{resultado['id']}"

    # [Actividad 10 - P27] Retorna la representacion JSON de la venta con `id`, `operacionId`, `vehiculoId`, `compradorId`, precio oficial y estado
    return resultado


# [Actividad 4] [Actividad 10 - P27] Endpoint de listado historico de ventas para clientes PWA React y Windows Forms
@router.get("/ventas")
def listar_ventas(u=Depends(identidad)):
    """Retorna el historial de ventas ordenado de la mas reciente a la mas antigua segun permisos RBAC."""
    # Abre una sesion corta de lectura sobre MySQL
    with SessionLocal() as db:
        # [Actividad 4] Permite visibilidad global a roles `ADMIN`, `OPERADOR` y `CONSULTA`
        if u.get("rol") in ("ADMIN", "OPERADOR", "CONSULTA"):
            # Recupera todas las ventas en orden descendente por identificador
            ventas = db.query(Venta).order_by(Venta.id.desc()).all()
        else:
            # Limita el historial unicamente a las ventas creadas por el usuario autenticado
            ventas = db.query(Venta).filter(Venta.usuario == u["usuario"]).order_by(Venta.id.desc()).all()

        # [Actividad 10 - P27] Formatea cada entidad ORM `Venta` con el contrato JSON uniforme `service.salida(v)`
        return [service.salida(v) for v in ventas]


# [Actividad 4] [Actividad 10 - P27] [Actividad 11] Endpoint de consulta puntual de una venta por ID
@router.get("/ventas/{id}")
def obtener(id: int, u=Depends(identidad)):
    """Consulta el detalle consolidado de una venta especifica validando existencia y permisos."""
    # Abre una sesion corta de solo lectura en MySQL
    with SessionLocal() as db:
        # Busca directamente la venta por su clave primaria `id`
        v = db.get(Venta, id)

        # [Actividad 4] [Actividad 11] Si la venta no existe, responde HTTP 404 RFC 7807
        if v is None:
            # Evita devolver `null` y comunica explicitamente recurso inexistente
            raise ApiError(404, "VENTA_NO_EXISTE", "Venta inexistente")

        # [Actividad 4] [Actividad 11] Si el usuario no es `ADMIN` ni `OPERADOR`, exige que sea propietario de la venta (P10 -> 403)
        if u.get("rol") not in ("ADMIN", "OPERADOR"):
            # Ejecuta la guardia horizontal `propietario()` contra `v.usuario`
            propietario(u, v.usuario)

        # [Actividad 10 - P27] Serializa y retorna la venta con sus identificadores transversales y estado actualizado
        return service.salida(v)
