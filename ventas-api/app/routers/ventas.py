# Soporte para anotaciones de tipos avanzadas en parametros de FastAPI
from typing import Annotated

# Componentes del enrutador, dependencias de seguridad y respuesta HTTP de FastAPI
from fastapi import APIRouter, Depends, Header, Response

# Fabrica de sesiones para interactuar con la base de datos
from app.db import SessionLocal
# Modelos ORM de base de datos
from app.models import Venta, Comprador
# Esquemas Pydantic de entrada para validacion de datos
from app.schemas import CrearVenta, CrearComprador
# Funciones de autenticacion y verificacion de roles/permisos
from app.security import identidad, vendedor, propietario
# Manejador de errores de la API
from app.errors import ApiError
# Servicio de logica de negocio para las ventas
from app.services import venta_service as service

# Instancia del enrutador de FastAPI con prefijo comun '/api'
router = APIRouter(prefix="/api")

# Endpoint para registrar un nuevo comprador en el sistema (retorna HTTP 201 Created)
@router.post("/compradores", status_code=201)
def crear_comprador(d: CrearComprador, u=Depends(identidad)):
    # Valida que el usuario autenticado tenga rol de vendedor (OPERADOR o ADMIN)
    vendedor(u)

    # Inicia una transaccion en la base de datos
    with SessionLocal.begin() as db:
        # Crea la instancia del comprador vinculandolo al usuario actual
        c = Comprador(usuario=u["usuario"], **d.model_dump())
        db.add(c)
        # Hace flush para generar y obtener el ID autoincremental sin cerrar la transaccion
        db.flush()
        # Retorna los datos basicos del comprador creado
        return {"id": c.id, "nombre": c.nombre, "correo": c.correo}

# Endpoint para registrar una venta con garantia de idempotencia
@router.post("/ventas")
def crear(d: CrearVenta, response: Response,
          # Requiere el encabezado Idempotency-Key con formato alfanumerico y longitud de 8 a 100 caracteres
          idempotency_key: Annotated[str, Header(
              min_length=8, max_length=100, pattern=r"^[A-Za-z0-9_-]+$")],
          u=Depends(identidad)):
    # Valida permisos de vendedor
    vendedor(u)

    # Ejecuta el flujo transaccional de venta en el servicio de negocio
    resultado, codigo = service.crear(d, idempotency_key, u)

    # Asigna el codigo HTTP dinamico (201 si fue creada, 200 si fue idempotente, 202 si esta pendiente)
    response.status_code = codigo

    # Establece el encabezado 'Location' apuntando a la URL del nuevo recurso creado
    response.headers["Location"] = f"/api/ventas/{resultado['id']}"

    # Retorna la representacion de la venta
    return resultado

# Endpoint para consultar los detalles de una venta segun su identificador
@router.get("/ventas/{id}")
def obtener(id: int, u=Depends(identidad)):
    # Abre sesion de solo lectura
    with SessionLocal() as db:
        # Busca la venta por ID primario
        v = db.get(Venta, id)

        # Si no existe, retorna error HTTP 404
        if v is None:
            raise ApiError(404, "VENTA_NO_EXISTE", "Venta inexistente")

        # Valida que solo el propietario de la venta o un ADMIN puedan consultarla
        propietario(u, v.usuario)

        # Formatea y retorna la venta
        return service.salida(v)
