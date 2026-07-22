const request = require('supertest');
const nock = require('nock');
const app = require('../../src/app');

const ORDER_SERVICE_URL = process.env.ORDER_SERVICE_URL || 'http://localhost:8081';

beforeEach(() => {
  nock.cleanAll();
});

afterAll(() => {
  nock.restore();
});

describe('POST /orders', () => {
  const validBody = {
    customerId: 'customer-1',
    productId: 'product-1',
    quantity: 2,
  };

  it('returns 201 when order-service creates the order', async () => {
    nock(ORDER_SERVICE_URL)
      .post('/orders')
      .reply(201, { id: 1, status: 'PENDING', ...validBody });

    const res = await request(app).post('/orders').send(validBody);

    expect(res.status).toBe(201);
    expect(res.body.id).toBe(1);
  });

  it('returns 400 when customerId is missing', async () => {
    const res = await request(app)
      .post('/orders')
      .send({ productId: 'product-1', quantity: 2 });

    expect(res.status).toBe(400);
    expect(res.body.error).toBeDefined();
  });

  it('returns 400 when productId is missing', async () => {
    const res = await request(app)
      .post('/orders')
      .send({ customerId: 'customer-1', quantity: 2 });

    expect(res.status).toBe(400);
  });

  it('returns 400 when quantity is not a positive integer', async () => {
    const res = await request(app)
      .post('/orders')
      .send({ customerId: 'customer-1', productId: 'product-1', quantity: -1 });

    expect(res.status).toBe(400);
  });

  it('returns 400 when quantity is zero', async () => {
    const res = await request(app)
      .post('/orders')
      .send({ customerId: 'customer-1', productId: 'product-1', quantity: 0 });

    expect(res.status).toBe(400);
  });

  it('propaga x-correlation-id existente hacia order-service', async () => {
    const correlationId = 'test-correlation-id';

    nock(ORDER_SERVICE_URL)
      .post('/orders')
      .matchHeader('x-correlation-id', correlationId)
      .reply(201, { id: 1, status: 'PENDING' });

    const res = await request(app)
      .post('/orders')
      .set('x-correlation-id', correlationId)
      .send(validBody);

    expect(res.status).toBe(201);
    expect(res.headers['x-correlation-id']).toBe(correlationId);
  });

  it('genera correlationId nuevo si el cliente no manda uno', async () => {
    nock(ORDER_SERVICE_URL)
      .post('/orders')
      .reply(201, { id: 1, status: 'PENDING' });

    const res = await request(app).post('/orders').send(validBody);

    expect(res.headers['x-correlation-id']).toBeDefined();
    expect(res.headers['x-correlation-id']).toMatch(
      /^[0-9a-f-]{36}$/
    );
  });

  it('retorna error en formato estandar cuando order-service falla', async () => {
    nock(ORDER_SERVICE_URL)
      .post('/orders')
      .reply(500, { error: 'INTERNAL_SERVER_ERROR', message: 'Something went wrong' });

    const res = await request(app).post('/orders').send(validBody);

    expect(res.status).toBe(500);
    expect(res.body).toMatchObject({
      error: expect.any(String),
      message: expect.any(String),
      timestamp: expect.any(String),
      correlationId: expect.any(String),
    });
  });
});

describe('GET /orders/:id', () => {
  it('returns 200 con la orden cuando existe', async () => {
    nock(ORDER_SERVICE_URL)
      .get('/orders/1')
      .reply(200, { id: 1, status: 'PENDING' });

    const res = await request(app).get('/orders/1');

    expect(res.status).toBe(200);
    expect(res.body.id).toBe(1);
  });

  it('retorna 404 en formato estandar cuando la orden no existe', async () => {
    nock(ORDER_SERVICE_URL)
      .get('/orders/999')
      .reply(404, {
        error: 'RESOURCE_NOT_FOUND',
        message: 'Order with id 999 not found',
      });

    const res = await request(app).get('/orders/999');

    expect(res.status).toBe(404);
    expect(res.body).toMatchObject({
      error: 'RESOURCE_NOT_FOUND',
      message: expect.any(String),
      timestamp: expect.any(String),
      correlationId: expect.any(String),
    });
  });

  it('propaga x-correlation-id hacia order-service', async () => {
    const correlationId = 'test-correlation-id';

    nock(ORDER_SERVICE_URL)
      .get('/orders/1')
      .matchHeader('x-correlation-id', correlationId)
      .reply(200, { id: 1 });

    const res = await request(app)
      .get('/orders/1')
      .set('x-correlation-id', correlationId);

    expect(res.status).toBe(200);
    expect(res.headers['x-correlation-id']).toBe(correlationId);
  });
});
