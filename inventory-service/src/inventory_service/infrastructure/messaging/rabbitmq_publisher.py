import json
import logging
import os

import aio_pika
from aio_pika import DeliveryMode, Message

from inventory_service.domain.model.inventory_checked_event import InventoryCheckedEvent

logger = logging.getLogger(__name__)

RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.environ.get("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.environ.get("RABBITMQ_PASS", "guest")

# mismo exchange que usa el order-service — todos los eventos del sistema
# fluyen por aquí, diferenciados por routing key
EXCHANGE_NAME = "orders.exchange"
ROUTING_KEY   = "inventory.checked"


class RabbitMqPublisher:

    def __init__(self):
        self._connection = None
        self._channel    = None
        self._exchange   = None

    async def start(self) -> None:
        # conexión independiente del consumer — mezclar publicación y consumo
        # en el mismo canal puede causar bloqueos bajo carga
        self._connection = await aio_pika.connect_robust(
            host=RABBITMQ_HOST,
            port=RABBITMQ_PORT,
            login=RABBITMQ_USER,
            password=RABBITMQ_PASS,
        )
        self._channel = await self._connection.channel()

        # declaramos el exchange — si ya existe (lo creó el consumer o el order-service)
        # RabbitMQ no hace nada; si no existe, lo crea
        self._exchange = await self._channel.declare_exchange(
            EXCHANGE_NAME,
            aio_pika.ExchangeType.TOPIC,
            durable=True,
        )
        logger.info("RabbitMQ publisher ready")

    async def publish_inventory_checked(self, event: InventoryCheckedEvent, correlation_id: str) -> None:
        body = json.dumps({
            "orderId":    str(event.order_id),
            "customerId": event.customer_id,
            "productId":  event.product_id,
            "approved":   event.approved,
            "reason":     event.reason,
        }).encode()

        message = Message(
            body,
            # PERSISTENT garantiza que el mensaje sobrevive un reinicio de RabbitMQ
            # antes de que el notification-service lo consuma
            delivery_mode=DeliveryMode.PERSISTENT,
            content_type="application/json",
            correlation_id=correlation_id,
        )

        await self._exchange.publish(message, routing_key=ROUTING_KEY)
        logger.info("inventory.checked published: order_id=%s approved=%s",
                    event.order_id, event.approved)

    async def stop(self) -> None:
        if self._connection:
            await self._connection.close()
            logger.info("RabbitMQ publisher connection closed")