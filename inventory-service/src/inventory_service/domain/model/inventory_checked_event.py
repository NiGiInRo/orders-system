from dataclasses import dataclass
from uuid import UUID


# Evento que este servicio publica — el notification-service lo consumirá.
# Es el equivalente de salida a lo que order.created es de entrada.
@dataclass
class InventoryCheckedEvent:
    order_id: UUID
    customer_id: str
    product_id: str
    approved: bool    # True = orden confirmada, False = orden rechazada
    reason: str       # mensaje legible para el log de notificaciones