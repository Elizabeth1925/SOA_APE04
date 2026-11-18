# Clases y utilidades de Pydantic para definicion y validacion de esquemas DTO (Data Transfer Objects)
from pydantic import BaseModel, ConfigDict, Field

# Esquema para validar los datos al registrar un nuevo comprador
class CrearComprador(BaseModel):
    # Configuracion del modelo:
    # extra="forbid": rechaza campos adicionales no especificados en el esquema
    # str_strip_whitespace=True: elimina espacios en blanco al inicio y al final de las cadenas
    model_config = ConfigDict(extra="forbid", str_strip_whitespace=True)

    # Documento de identidad del comprador (entre 3 y 30 caracteres)
    documento: str = Field(min_length=3, max_length=30)

    # Nombre completo del comprador (entre 2 y 120 caracteres)
    nombre: str = Field(min_length=2, max_length=120)

    # Correo electronico valido con expresion regular (entre 5 y 200 caracteres)
    correo: str = Field(min_length=5, max_length=200,
                        pattern=r"^[^@\s]+@[^@\s]+\.[^@\s]+$")

# Esquema para validar la peticion de creacion de una nueva venta
class CrearVenta(BaseModel):
    # extra="forbid" prohibe atributos adicionales en el JSON de entrada
    model_config = ConfigDict(extra="forbid")

    # Identificador del vehiculo en Java; debe ser un entero estrictamente mayor a 0
    vehiculoId: int = Field(gt=0, strict=True)

    # Identificador del comprador registrado previamente; debe ser estrictamente mayor a 0
    compradorId: int = Field(gt=0, strict=True)
