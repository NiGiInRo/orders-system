import asyncio
import logging

from inventory_service.domain.model.order_created_event import OrderCreatedEvent
from inventory_service.domain.model.stock_check_result import StockCheckResult
from inventory_service.infrastructure.persistence.product_repository_adapter import ProductRepositoryAdapter

logger = logging.getLogger(__name__)


class CheckStockService:

    def __init__(self, product_repository: ProductRepositoryAdapter):
        # recibe el repositorio como dependencia — no lo instancia él mismo
        self._repo = product_repository

    async def check_stock(self, event: OrderCreatedEvent) -> StockCheckResult:
        # asyncio.to_thread corre el código síncrono de SQLAlchemy en un thread separado
        # para no bloquear el event loop de FastAPI mientras espera la DB
        product, approved = await asyncio.to_thread(
            self._repo.check_and_reserve,
            event.product_id,
            event.quantity,
        )

        reason = "Stock reserved successfully" if approved else (
            "Product not found" if product is None else
            f"Insufficient stock: available={product.stock_quantity}, requested={event.quantity}"
        )

        result = StockCheckResult(
            order_id=event.order_id,
            customer_id=event.customer_id,
            product_id=event.product_id,
            approved=approved,
            reason=reason,
        )

        logger.info(
            "Stock check completed",
            extra={"order_id": str(event.order_id), "approved": approved, "reason": reason}
        )

        return result