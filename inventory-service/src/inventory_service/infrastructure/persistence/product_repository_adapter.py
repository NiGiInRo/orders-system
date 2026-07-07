import logging
from typing import Optional

from sqlalchemy.orm import Session

from inventory_service.domain.model.product import Product
from inventory_service.infrastructure.persistence.database import SessionLocal
from inventory_service.infrastructure.persistence.product_entity import ProductEntity

logger = logging.getLogger(__name__)


class ProductRepositoryAdapter:

    def check_and_reserve(self, sku: str, quantity: int) -> tuple[Optional[Product], bool]:
        # toda la operación ocurre dentro de una sola transacción
        with SessionLocal() as session:
            with session.begin():
                # with_for_update() genera SELECT FOR UPDATE en PostgreSQL.
                # Bloquea la fila hasta que el commit libere el lock.
                # Si dos requests llegan simultáneamente, el segundo espera aquí.
                entity = (
                    session.query(ProductEntity)
                    .filter_by(sku=sku)
                    .with_for_update()
                    .first()
                )

                if entity is None:
                    logger.warning("Product not found for sku=%s", sku)
                    return None, False

                # mapeamos la entidad al objeto de dominio para que la lógica viva ahí
                product = Product(
                    id=entity.id,
                    sku=entity.sku,
                    name=entity.name,
                    stock_quantity=entity.stock_quantity,
                )

                if not product.has_sufficient_stock(quantity):
                    logger.info("Insufficient stock sku=%s available=%d requested=%d",
                                sku, product.stock_quantity, quantity)
                    return product, False

                # el dominio hace la reserva — descuenta el stock en el objeto
                product.reserve(quantity)

                # persistimos el nuevo stock — el commit al salir del with libera el lock
                entity.stock_quantity = product.stock_quantity

                logger.info("Stock reserved sku=%s remaining=%d",
                            sku, product.stock_quantity)
                return product, True