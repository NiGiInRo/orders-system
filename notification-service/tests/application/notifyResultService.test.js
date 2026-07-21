const { NotifyResultService } = require('../../src/application/notifyResultService');

function makeEvent(overrides = {}) {
  return {
    orderId: 'order-1',
    customerId: 'customer-1',
    productId: 'PROD-001',
    approved: true,
    reason: 'Stock reserved successfully',
    ...overrides,
  };
}

describe('NotifyResultService', () => {
  let notificationPort;
  let service;

  beforeEach(() => {
    // mock del port de salida — el service no debe saber que es pino/JSON
    notificationPort = { notify: jest.fn().mockResolvedValue(undefined) };
    service = new NotifyResultService(notificationPort);
  });

  test('maps approved=true to result APPROVED', async () => {
    await service.notifyResult(makeEvent({ approved: true }), 'corr-1');

    expect(notificationPort.notify).toHaveBeenCalledTimes(1);
    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(logEntry.result).toBe('APPROVED');
  });

  test('maps approved=false to result REJECTED', async () => {
    await service.notifyResult(makeEvent({ approved: false }), 'corr-1');

    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(logEntry.result).toBe('REJECTED');
  });

  test('forwards correlationId unchanged', async () => {
    await service.notifyResult(makeEvent(), 'corr-abc-123');

    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(logEntry.correlationId).toBe('corr-abc-123');
  });

  test('carries orderId from the event', async () => {
    await service.notifyResult(makeEvent({ orderId: 'order-999' }), 'corr-1');

    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(logEntry.orderId).toBe('order-999');
  });

  test('generates a valid ISO timestamp', async () => {
    await service.notifyResult(makeEvent(), 'corr-1');

    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(() => new Date(logEntry.timestamp).toISOString()).not.toThrow();
    expect(logEntry.timestamp).toBe(new Date(logEntry.timestamp).toISOString());
  });

  test('log entry has exactly the 4 required fields', async () => {
    await service.notifyResult(makeEvent(), 'corr-1');

    const logEntry = notificationPort.notify.mock.calls[0][0];
    expect(Object.keys(logEntry).sort()).toEqual(
      ['correlationId', 'orderId', 'result', 'timestamp'].sort()
    );
  });
});
