from unittest.mock import AsyncMock, MagicMock
from uuid import uuid4

import pytest

from inventory_service.application.check_stock_service import CheckStockService
from inventory_service.domain.model.order_created_event import OrderCreatedEvent
from inventory_service.domain.model.product import Product


# ── helpers ───────────────────────────────────────────────────────────────────

def make_event(product_id: str = "PROD-001", quantity: int = 2) -> OrderCreatedEvent:
    return OrderCreatedEvent(
        order_id=uuid4(),
        customer_id="cliente-1",
        product_id=product_id,
        quantity=quantity,
    )


def make_product(stock_quantity: int) -> Product:
    return Product(id=uuid4(), sku="PROD-001", name="Test Product", stock_quantity=stock_quantity)


# ── fixtures ──────────────────────────────────────────────────────────────────

@pytest.fixture
def mock_repository():
    # MagicMock simula el ProductRepositoryAdapter sin tocar la DB
    return MagicMock()


@pytest.fixture
def mock_publisher():
    publisher = MagicMock()
    # publish_inventory_checked es async — necesita AsyncMock para poder hacer await
    publisher.publish_inventory_checked = AsyncMock()
    return publisher


@pytest.fixture
def service(mock_repository, mock_publisher):
    return CheckStockService(mock_repository, mock_publisher)


# ── tests ─────────────────────────────────────────────────────────────────────

class TestCheckStockService:

    async def test_approved_order_publishes_inventory_checked_with_true(
        self, service, mock_repository, mock_publisher
    ):
        mock_repository.check_and_reserve.return_value = (make_product(10), True)
        event = make_event(quantity=2)

        result = await service.check_stock(event)

        assert result.approved is True
        mock_publisher.publish_inventory_checked.assert_called_once()
        published = mock_publisher.publish_inventory_checked.call_args[0][0]
        assert published.approved is True
        assert published.order_id == event.order_id

    async def test_rejected_order_insufficient_stock_publishes_false(
        self, service, mock_repository, mock_publisher
    ):
        mock_repository.check_and_reserve.return_value = (make_product(0), False)
        event = make_event(quantity=2)

        result = await service.check_stock(event)

        assert result.approved is False
        assert "Insufficient stock" in result.reason
        published = mock_publisher.publish_inventory_checked.call_args[0][0]
        assert published.approved is False

    async def test_rejected_order_product_not_found_publishes_false(
        self, service, mock_repository, mock_publisher
    ):
        # repo devuelve (None, False) cuando el SKU no existe en DB
        mock_repository.check_and_reserve.return_value = (None, False)
        event = make_event(product_id="PROD-999")

        result = await service.check_stock(event)

        assert result.approved is False
        assert result.reason == "Product not found"
        mock_publisher.publish_inventory_checked.assert_called_once()

    async def test_result_carries_correct_order_metadata(
        self, service, mock_repository, mock_publisher
    ):
        mock_repository.check_and_reserve.return_value = (make_product(5), True)
        event = make_event()

        result = await service.check_stock(event)

        # el resultado debe propagar los IDs del evento original
        assert result.order_id == event.order_id
        assert result.product_id == event.product_id
        assert result.customer_id == event.customer_id

    async def test_publisher_receives_correct_customer_and_product(
        self, service, mock_repository, mock_publisher
    ):
        mock_repository.check_and_reserve.return_value = (make_product(5), True)
        event = make_event(product_id="PROD-002")

        await service.check_stock(event)

        published = mock_publisher.publish_inventory_checked.call_args[0][0]
        assert published.customer_id == "cliente-1"
        assert published.product_id == "PROD-002"
