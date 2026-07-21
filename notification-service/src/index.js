require('dotenv').config();

const express = require('express');
const pino = require('pino');
const { startConsumer } = require('./infrastructure/messaging/rabbitMqConsumer');
const { JsonLogNotifier } = require('./infrastructure/logging/jsonLogNotifier');
const { NotifyResultService } = require('./application/notifyResultService');

const logger = pino();
const app = express();
const port = process.env.PORT || 8083;

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(port, () => {
  logger.info(`notification-service listening on port ${port}`);
});

// composición: el dominio (NotifyResultService) no sabe que el canal es un log JSON con pino
const notifier = new JsonLogNotifier(logger);
const notifyResultService = new NotifyResultService(notifier);

startConsumer((event, correlationId) => notifyResultService.notifyResult(event, correlationId))
  .catch((err) => {
    logger.error({ err }, 'Failed to start RabbitMQ consumer');
    process.exit(1);
  });