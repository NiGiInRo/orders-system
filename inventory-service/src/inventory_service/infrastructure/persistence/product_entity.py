from sqlalchemy import CheckConstraint, Column, Integer, String
from sqlalchemy.dialects.postgresql import UUID

from inventory_service.infrastructure.persistence.database import Base


# ProductEntity es la representación de la tabla en SQLAlchemy.
# Es infraestructura pura — el dominio no la conoce.
# Equivale al OrderEntity de Java con anotaciones JPA.
class ProductEntity(Base):
    __tablename__ = "products"

    id             = Column(UUID(as_uuid=True), primary_key=True)
    sku            = Column(String(50),  nullable=False, unique=True)
    name           = Column(String(255), nullable=False)
    stock_quantity = Column(Integer,     nullable=False,
                            # segunda línea de defensa — la primera es domain.Product.reserve()
                            info={"check": CheckConstraint("stock_quantity >= 0")})