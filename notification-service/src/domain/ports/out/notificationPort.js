class NotificationPort {
  // eslint-disable-next-line no-unused-vars
  async notify(logEntry) {
    throw new Error('NotificationPort.notify must be implemented by an adapter');
  }
}

module.exports = { NotificationPort };