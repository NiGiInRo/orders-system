const express = require('express');
const { createOrder, getOrder } = require('../clients/orderServiceClient');
const validateOrder = require('../middleware/validateOrder');

const router = express.Router();

router.post('/', validateOrder, async (req, res, next) => {
  try {
    const order = await createOrder(req.body, req.correlationId);
    res.status(201).json(order);
  } catch (err) {
    next(err);
  }
});

router.get('/:id', async (req, res, next) => {
  try {
    const order = await getOrder(req.params.id, req.correlationId);
    res.json(order);
  } catch (err) {
    next(err);
  }
});

module.exports = router;
