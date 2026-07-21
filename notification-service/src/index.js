require('dotenv').config();

const express = require('express');
const pino = require('pino');
const { startConsumer } = require('./infrastructure/messaging/rabbitMqConsumer');

const logger = pino();
const app = express();
const port = process.env.PORT || 8083;

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(port, () => {
  logger.info(`notification-service listening on port ${port}`);
});

// HU-17 reemplaza este handler por el que arma el log estructurado exacto
// (correlationId, orderId, result, timestamp)
async function handleInventoryChecked(event, correlationId) {
  logger.info({ correlationId, orderId: event.orderId }, 'inventory.checked received');
}

startConsumer(handleInventoryChecked).catch((err) => {
  logger.error({ err }, 'Failed to start RabbitMQ consumer');
  process.exit(1);
});
