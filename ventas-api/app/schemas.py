# Clases y utilidades de Pydantic para definicion y validacion de esquemas DTO (Data Transfer Objects)
from typing import Optional
from pydantic import BaseModel, ConfigDict, Field, model_validator

# Esquema para validar los datos al registrar un nuevo comprador
class CrearComprador(BaseModel):
    model_config = ConfigDict(extra="ignore", str_strip_whitespace=True)

    documento: Optional[str] = None
    cedula: Optional[str] = None
    nombre: str = Field(min_length=2, max_length=120)
    correo: Optional[str] = None
    email: Optional[str] = None
    telefono: Optional[str] = None

    @model_validator(mode="after")
    def normalizar_campos(self):
        if not self.documento and self.cedula:
            self.documento = self.cedula
        if not self.correo and self.email:
            self.correo = self.email
        if not self.documento or len(self.documento.strip()) < 3:
            raise ValueError("El documento o cedula debe tener al menos 3 caracteres")
        if not self.correo or "@" not in self.correo:
            raise ValueError("Debe proporcionar un correo electronico valido")
        return self

# Esquema para validar la peticion de creacion de una nueva venta
class CrearVenta(BaseModel):
    model_config = ConfigDict(extra="ignore")

    vehiculoId: Optional[int] = None
    vehiculo_id: Optional[int] = None
    compradorId: Optional[int] = None
    comprador_id: Optional[int] = None
    precio: Optional[float] = None

    @model_validator(mode="after")
    def normalizar_campos(self):
        if self.vehiculoId is None and self.vehiculo_id is not None:
            self.vehiculoId = self.vehiculo_id
        if self.compradorId is None and self.comprador_id is not None:
            self.compradorId = self.comprador_id
        if self.vehiculoId is None or self.vehiculoId <= 0:
            raise ValueError("vehiculoId debe ser un entero estrictamente mayor a 0")
        if self.compradorId is None or self.compradorId <= 0:
            raise ValueError("compradorId debe ser un entero estrictamente mayor a 0")
        return self
