import logging

from inventory_service.domain.model.order_created_event import OrderCreatedEvent

logger = logging.getLogger(__name__)


class CheckStockService:

    # HU-13 reemplazará el body de este método con:
    # 1. buscar el producto por sku en DB
    # 2. verificar y reservar stock
    # 3. publicar evento inventory.checked
    async def check_stock(self, event: OrderCreatedEvent) -> None:
        logger.info(
            "order.created received — stock check pending (HU-13)",
            extra={
                "order_id": str(event.order_id),
                "product_id": event.product_id,
                "quantity": event.quantity,
            }
        )