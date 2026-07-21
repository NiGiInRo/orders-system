const amqp = require('amqplib');

const RABBITMQ_HOST = process.env.RABBITMQ_HOST || 'localhost';
const RABBITMQ_PORT = process.env.RABBITMQ_PORT || 5672;
const RABBITMQ_USER = process.env.RABBITMQ_USER || 'guest';
const RABBITMQ_PASS = process.env.RABBITMQ_PASS || 'guest';

// mismo exchange que order-service e inventory-service — todos los eventos
// del sistema fluyen por acá, diferenciados por routing key
const EXCHANGE_NAME = 'orders.exchange';
const ROUTING_KEY = 'inventory.checked';

// nombre de nuestra cola — notification-service es dueño de ella,
// igual que cada servicio declara y es dueño de la suya
const QUEUE_NAME = 'notification.inventory.checked.queue';

async function startConsumer(messageHandler) {
  const connectionUrl = `amqp://${RABBITMQ_USER}:${RABBITMQ_PASS}@${RABBITMQ_HOST}:${RABBITMQ_PORT}`;
  const connection = await amqp.connect(connectionUrl);
  const channel = await connection.createChannel();

  // procesa un mensaje a la vez antes de hacer ack — evita que el consumer
  // se llene de mensajes sin procesar si el handler es lento
  await channel.prefetch(1);

  // declaramos el exchange igual que los otros servicios: topic, durable.
  // Si ya existe con los mismos parámetros, no hace nada.
  await channel.assertExchange(EXCHANGE_NAME, 'topic', { durable: true });

  // declaramos nuestra cola — durable=true sobrevive un reinicio de RabbitMQ
  const queue = await channel.assertQueue(QUEUE_NAME, { durable: true });

  // vinculamos la cola al exchange con el routing key del publisher
  await channel.bindQueue(queue.queue, EXCHANGE_NAME, ROUTING_KEY);

  channel.consume(queue.queue, async (msg) => {
    if (!msg) return;

    try {
      const event = JSON.parse(msg.content.toString());

      // correlationId viaja como propiedad nativa de AMQP, no en el body
      // (ver ADR-003-correlation-id-propagation.md)
      const correlationId = msg.properties.correlationId;

      await messageHandler(event, correlationId);

      // ack manual: solo confirmamos si el handler no lanzó excepción
      channel.ack(msg);
    } catch (err) {
      // si el handler falla, el mensaje vuelve a la cola (requeue) para reintentar
      channel.nack(msg, false, true);
    }
  });

  return connection;
}

module.exports = { startConsumer };
