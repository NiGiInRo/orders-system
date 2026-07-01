import asyncio
import json
import logging
import os
from uuid import UUID

import aio_pika
from aio_pika import IncomingMessage

from inventory_service.domain.model.order_created_event import OrderCreatedEvent

logger = logging.getLogger(__name__)

# Variables de entorno con defaults para desarrollo local sin Docker.
# En Docker, el docker-compose las inyecta con los valores reales.
RABBITMQ_HOST = os.environ.get("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.environ.get("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.environ.get("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.environ.get("RABBITMQ_PASS", "guest")

# Deben coincidir exactamente con lo que declaró el order-service en Java.
# Si cambia uno, el mensaje no llega — es un contrato implícito entre servicios.
EXCHANGE_NAME = "orders.exchange"
ROUTING_KEY = "order.created"

# Nombre de nuestra cola — el inventory-service es dueño de ella.
# Cada servicio declara su propia cola y la vincula al exchange.
QUEUE_NAME = "inventory.order.created.queue"


async def start_consumer(message_handler) -> None:
    # connect_robust reintenta la conexión automáticamente si RabbitMQ se cae y vuelve.
    # Es el equivalente al retry automático de Spring AMQP.
    connection = await aio_pika.connect_robust(
        host=RABBITMQ_HOST,
        port=RABBITMQ_PORT,
        login=RABBITMQ_USER,
        password=RABBITMQ_PASS,
    )

    channel = await connection.channel()

    # prefetch_count=1: procesa un mensaje a la vez antes de hacer ack.
    # Evita que el consumer se llene de mensajes sin procesar si la lógica es lenta.
    await channel.set_qos(prefetch_count=1)

    # Declaramos el exchange igual que Java: tipo topic, durable.
    # Si ya existe con los mismos parámetros, RabbitMQ no hace nada.
    # Si existe con parámetros distintos, lanza error — el contrato es el exchange.
    exchange = await channel.declare_exchange(
        EXCHANGE_NAME,
        aio_pika.ExchangeType.TOPIC,
        durable=True,
    )

    # Declaramos nuestra cola — durable=True garantiza que sobrevive un reinicio de RabbitMQ.
    # Si el servicio cae mientras hay mensajes pendientes, no se pierden.
    queue = await channel.declare_queue(QUEUE_NAME, durable=True)

    # Vinculamos la cola al exchange con el routing key del publisher.
    # A partir de aquí, todo mensaje publicado con "order.created" llega a esta cola.
    await queue.bind(exchange, routing_key=ROUTING_KEY)

    # Función interna que maneja cada mensaje entrante.
    async def on_message(message: IncomingMessage) -> None:
        # message.process() hace ack automático si el bloque termina sin excepción.
        # Si hay una excepción, el mensaje vuelve a la cola (nack automático).
        async with message.process():
            body = json.loads(message.body.decode())

            # Parseamos el JSON a nuestro dataclass de dominio.
            # Las keys del JSON vienen en camelCase porque Jackson (Java) las serializa así.
            event = OrderCreatedEvent(
                order_id=UUID(body["orderId"]),
                customer_id=body["customerId"],
                product_id=body["productId"],
                quantity=body["quantity"],
            )

            # Delegamos al handler inyectado — el consumer no sabe qué hace con el evento.
            await message_handler(event)

    await queue.consume(on_message)
    logger.info("RabbitMQ consumer started — listening on queue '%s'", QUEUE_NAME)

    # Mantiene la corrutina viva indefinidamente.
    # Cuando el lifespan de FastAPI termina, cancela esta tarea y el Future lanza CancelledError.
    await asyncio.Future()