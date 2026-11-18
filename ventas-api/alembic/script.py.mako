## Plantilla Mako utilizada por Alembic para generar automaticamente nuevos archivos de migracion
"""${message}

Revision ID: ${up_revision}
Revises: ${down_revision | comma,n}
Create Date: ${create_date}

"""
# Tipos para anotaciones de tipo compatibles con versiones recientes de Python
from typing import Sequence, Union

# Operaciones DDL de Alembic y tipos de datos de SQLAlchemy
from alembic import op
import sqlalchemy as sa
${imports if imports else ""}

# Identificadores de revision autogenerados por Alembic
revision: str = ${repr(up_revision)}
down_revision: Union[str, Sequence[str], None] = ${repr(down_revision)}
branch_labels: Union[str, Sequence[str], None] = ${repr(branch_labels)}
depends_on: Union[str, Sequence[str], None] = ${repr(depends_on)}

# Funcion donde se colocan las operaciones para aplicar la migracion (crear tablas, columnas, etc.)
def upgrade() -> None:
    """Upgrade schema."""
    ${upgrades if upgrades else "pass"}

# Funcion donde se colocan las operaciones para deshacer o revertir la migracion
def downgrade() -> None:
    """Downgrade schema."""
    ${downgrades if downgrades else "pass"}
