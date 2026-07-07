from dataclasses import dataclass
from uuid import UUID


# Espejo del record Java OrderCreatedEvent — representa el hecho de que
# el order-service creó una orden y nosotros debemos verificar el stock.
# Es solo datos: sin lógica, sin imports de infraestructura.
@dataclass
class OrderCreatedEvent:
    order_id: UUID    # identificador único de la orden
    customer_id: str  # quién hizo la orden (para el evento de respuesta)
    product_id: str   # identificador del producto — se mapea contra 'sku' en nuestra DB
    quantity: int     # cuántas unidades pidió el cliente