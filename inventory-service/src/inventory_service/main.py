import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from inventory_service.application.check_stock_service import CheckStockService
from inventory_service.infrastructure.messaging.rabbitmq_consumer import start_consumer
from inventory_service.infrastructure.messaging.rabbitmq_publisher import RabbitMqPublisher
from inventory_service.infrastructure.persistence.product_repository_adapter import ProductRepositoryAdapter

logging.basicConfig(
    level=logging.INFO,
    format='{"time": "%(asctime)s", "level": "%(levelname)s", "logger": "%(name)s", "message": "%(message)s"}',
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # 1. publisher arranca primero — el servicio necesita poder publicar
    #    antes de empezar a consumir mensajes
    publisher = RabbitMqPublisher()
    await publisher.start()

    # 2. construimos el grafo completo de dependencias
    product_repository  = ProductRepositoryAdapter()
    check_stock_service = CheckStockService(product_repository, publisher)

    # 3. consumer arranca último — ya tiene todo lo que necesita
    consumer_task = asyncio.create_task(
        start_consumer(check_stock_service.check_stock)
    )

    logger.info("Inventory service started")
    yield

    # shutdown en orden inverso al arranque
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        pass

    await publisher.stop()


app = FastAPI(title="Inventory Service", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "ok"}