# dataclass es un decorador de la librería estándar de Python que genera
# automáticamente __init__, __repr__ y __eq__ basándose en los campos declarados.
# Equivale a un POJO en Java con Lombok @Data, pero sin dependencias externas.
from dataclasses import dataclass
from uuid import UUID


# @dataclass elimina el boilerplate de escribir __init__ manualmente.
# Esta clase NO importa nada de SQLAlchemy ni de FastAPI — es dominio puro.
# La regla de arquitectura hexagonal es que el dominio no conoce la infraestructura.
@dataclass
class Product:
    id: UUID           # identificador técnico único (generado por la DB)
    sku: str           # identificador de negocio, ej: "PROD-001" — lo usa el Order Service en el evento
    name: str          # nombre legible del producto
    stock_quantity: int  # unidades disponibles; si llega a 0, las órdenes se rechazan

    def has_sufficient_stock(self, quantity: int) -> bool:
    # la decisión de negocio vive aquí, no en el servicio de aplicación
     return self.stock_quantity >= quantity

    def reserve(self, quantity: int) -> None:
    # el dominio protege su propio invariante — nunca stock negativo
        if not self.has_sufficient_stock(quantity):
            raise ValueError(
                f"Insufficient stock: available={self.stock_quantity}, requested={quantity}"
            )
        self.stock_quantity -= quantity