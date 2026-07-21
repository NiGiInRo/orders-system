class NotifyResultService {
  constructor(notificationPort) {
    this._notificationPort = notificationPort;
  }

  async notifyResult(event, correlationId) {
    const logEntry = {
      correlationId,
      orderId: event.orderId,
      result: event.approved ? 'APPROVED' : 'REJECTED',
      timestamp: new Date().toISOString(),
    };

    await this._notificationPort.notify(logEntry);
  }
}

module.exports = { NotifyResultService };