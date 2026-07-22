const { randomUUID } = require('crypto');

function correlationId(req, res, next) {
  req.correlationId = req.headers['x-correlation-id'] || randomUUID();
  res.setHeader('x-correlation-id', req.correlationId);
  next();
}

module.exports = correlationId;
