from typing import Optional, Protocol

from inventory_service.domain.model.product import Product


# Protocol es la forma Pythonica de definir una interfaz.
# Equivale al interface OrderRepository de Java.
# La capa de aplicación depende de este contrato, no de SQLAlchemy.
class ProductRepository(Protocol):

    def find_by_sku_with_lock(self, sku: str, session) -> Optional[Product]:
        # busca el producto y bloquea la fila para escritura (SELECT FOR UPDATE)
        ...

    def update_stock(self, product: Product, session) -> None:
        # persiste el nuevo stock_quantity del producto
        ...