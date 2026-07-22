const axios = require('axios');

const client = axios.create({
  baseURL: process.env.ORDER_SERVICE_URL || 'http://localhost:8081',
  timeout: 5000,
});

async function createOrder(body, correlationId) {
  const response = await client.post('/orders', body, {
    headers: { 'x-correlation-id': correlationId },
  });
  return response.data;
}

async function getOrder(id, correlationId) {
  const response = await client.get(`/orders/${id}`, {
    headers: { 'x-correlation-id': correlationId },
  });
  return response.data;
}

module.exports = { createOrder, getOrder };
