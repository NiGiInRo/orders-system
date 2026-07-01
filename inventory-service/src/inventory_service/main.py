import asyncio
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from inventory_service.application.check_stock_service import CheckStockService
from inventory_service.infrastructure.messaging.rabbitmq_consumer import start_consumer

# Log estructurado básico — cada línea es parseable como JSON por herramientas de observabilidad.
logging.basicConfig(
    level=logging.INFO,
    format='{"time": "%(asctime)s", "level": "%(levelname)s", "logger": "%(name)s", "message": "%(message)s"}',
)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    # --- STARTUP ---
    # Instanciamos el servicio de aplicación que el consumer va a llamar.
    check_stock_service = CheckStockService()

    # Lanzamos el consumer como tarea en background — no bloquea el arranque de FastAPI.
    consumer_task = asyncio.create_task(
        start_consumer(check_stock_service.check_stock)
    )
    logger.info("Inventory service started")

    yield  # FastAPI atiende requests mientras el consumer corre en paralelo

    # --- SHUTDOWN ---
    # Al cerrar el proceso, cancelamos el consumer limpiamente.
    consumer_task.cancel()
    try:
        await consumer_task
    except asyncio.CancelledError:
        logger.info("RabbitMQ consumer stopped cleanly")


app = FastAPI(title="Inventory Service", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health():
    return {"status": "ok"}