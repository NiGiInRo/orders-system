import asyncio
import logging

from inventory_service.domain.model.inventory_checked_event import InventoryCheckedEvent
from inventory_service.domain.model.order_created_event import OrderCreatedEvent
from inventory_service.domain.model.stock_check_result import StockCheckResult
from inventory_service.infrastructure.messaging.rabbitmq_publisher import RabbitMqPublisher
from inventory_service.infrastructure.persistence.product_repository_adapter import ProductRepositoryAdapter

logger = logging.getLogger(__name__)


class CheckStockService:

    def __init__(self, product_repository: ProductRepositoryAdapter, publisher: RabbitMqPublisher):
        self._repo      = product_repository
        self._publisher = publisher

    async def check_stock(self, event: OrderCreatedEvent) -> StockCheckResult:
        product, approved = await asyncio.to_thread(
            self._repo.check_and_reserve,
            event.product_id,
            event.quantity,
        )

        reason = (
            "Stock reserved successfully" if approved else
            "Product not found"           if product is None else
            f"Insufficient stock: available={product.stock_quantity}, requested={event.quantity}"
        )

        result = StockCheckResult(
            order_id=event.order_id,
            customer_id=event.customer_id,
            product_id=event.product_id,
            approved=approved,
            reason=reason,
        )

        # publica el resultado — el notification-service lo consumirá
        await self._publisher.publish_inventory_checked(
            InventoryCheckedEvent(
                order_id=result.order_id,
                customer_id=result.customer_id,
                product_id=result.product_id,
                approved=result.approved,
                reason=result.reason,
            )
        )

        return result