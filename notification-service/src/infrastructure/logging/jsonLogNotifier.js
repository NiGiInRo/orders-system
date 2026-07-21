const { NotificationPort } = require('../../domain/ports/out/notificationPort');

class JsonLogNotifier extends NotificationPort {
  constructor(logger) {
    super();
    this._logger = logger;
  }

  async notify(logEntry) {
    this._logger.info(logEntry);
  }
}

module.exports = { JsonLogNotifier };