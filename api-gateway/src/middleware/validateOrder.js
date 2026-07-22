function validateOrder(req, res, next) {
  const { customerId, productId, quantity } = req.body;

  if (!customerId || typeof customerId !== 'string' || customerId.trim() === '') {
    return res.status(400).json({ error: 'customerId is required and must be a non-empty string' });
  }

  if (!productId || typeof productId !== 'string' || productId.trim() === '') {
    return res.status(400).json({ error: 'productId is required and must be a non-empty string' });
  }

  if (quantity === undefined || !Number.isInteger(quantity) || quantity <= 0) {
    return res.status(400).json({ error: 'quantity is required and must be a positive integer' });
  }

  next();
}

module.exports = validateOrder;