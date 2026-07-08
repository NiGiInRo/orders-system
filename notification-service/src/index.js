require('dotenv').config();

const express = require('express');
const pino = require('pino');

const logger = pino();
const app = express();
const port = process.env.PORT || 8083;

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(port, () => {
  logger.info(`notification-service listening on port ${port}`);
});

// HU-16: acá arranca el consumer de RabbitMQ que escucha inventory.checked
