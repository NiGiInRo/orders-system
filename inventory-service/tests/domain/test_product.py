import pytest
from uuid import uuid4

from inventory_service.domain.model.product import Product


# helper para no repetir la construcción del objeto en cada test
def make_product(stock_quantity: int) -> Product:
    return Product(
        id=uuid4(),
        sku="PROD-001",
        name="Test Product",
        stock_quantity=stock_quantity,
    )


class TestHasSufficientStock:

    def test_returns_true_when_stock_exceeds_quantity(self):
        product = make_product(10)
        assert product.has_sufficient_stock(5) is True

    def test_returns_true_when_stock_equals_requested_quantity(self):
        # caso borde: exactamente el stock disponible debe aprobarse
        product = make_product(5)
        assert product.has_sufficient_stock(5) is True

    def test_returns_false_when_stock_is_less_than_quantity(self):
        product = make_product(3)
        assert product.has_sufficient_stock(5) is False

    def test_returns_false_when_stock_is_zero(self):
        product = make_product(0)
        assert product.has_sufficient_stock(1) is False


class TestReserve:

    def test_decrements_stock_when_sufficient(self):
        product = make_product(10)
        product.reserve(3)
        assert product.stock_quantity == 7

    def test_decrements_stock_to_zero_when_exact_quantity(self):
        # caso borde: reservar exactamente lo disponible debe dejar stock en 0
        product = make_product(5)
        product.reserve(5)
        assert product.stock_quantity == 0

    def test_raises_when_stock_is_insufficient(self):
        product = make_product(2)
        with pytest.raises(ValueError):
            product.reserve(5)

    def test_raises_when_stock_is_zero(self):
        product = make_product(0)
        with pytest.raises(ValueError):
            product.reserve(1)

    def test_stock_unchanged_when_reserve_fails(self):
        # garantiza que una reserva fallida no deja el stock en estado inconsistente
        product = make_product(2)
        with pytest.raises(ValueError):
            product.reserve(5)
        assert product.stock_quantity == 2