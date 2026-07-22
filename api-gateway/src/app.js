const express = require('express');
const pinoHttp = require('pino-http');
const logger = require('./logger');
const correlationIdMiddleware = require('./middleware/correlationId');
const orderRoutes = require('./routes/orders');

const app = express();

app.use(express.json());
app.use(correlationIdMiddleware);
app.use(pinoHttp({
  logger,
  customProps: (req) => ({ correlationId: req.correlationId }),
}));

app.get('/health', (_req, res) => {
  res.json({ status: 'ok', service: 'api-gateway' });
});

app.use('/orders', orderRoutes);

app.use((err, req, res, _next) => {
  const status = err.response?.status || 500;

  req.log.error({ err }, 'Request failed');

  res.status(status).json({
    error: err.response?.data?.error || 'INTERNAL_SERVER_ERROR',
    message: err.response?.data?.message || err.message,
    timestamp: new Date().toISOString(),
    correlationId: req.correlationId,
  });
});

module.exports = app;
