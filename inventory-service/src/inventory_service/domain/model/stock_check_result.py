from dataclasses import dataclass
from uuid import UUID


# Representa el resultado de verificar el stock para una orden.
# Este objeto es lo que HU-14 va a convertir en el evento inventory.checked.
@dataclass
class StockCheckResult:
    order_id: UUID
    customer_id: str
    product_id: str
    approved: bool          # True = stock reservado, False = stock insuficiente
    reason: str = ""        # mensaje legible para el evento de respuesta