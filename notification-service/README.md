# Notification Service

Consume el evento `inventory.checked` desde RabbitMQ y registra el resultado
como log estructurado en JSON, incluyendo `correlationId` para trazabilidad
end-to-end (ver `docs/architecture/adr/ADR-003-correlation-id-propagation.md`).

## Correr localmente

```bash
npm install
cp .env.example .env
npm start
```

Health check: `GET http://localhost:8083/health`

## Variables de entorno

| Variable | Descripción |
|---|---|
| `RABBITMQ_HOST` | Host de RabbitMQ |
| `RABBITMQ_PORT` | Puerto de RabbitMQ |
| `RABBITMQ_USER` | Usuario de RabbitMQ |
| `RABBITMQ_PASS` | Password de RabbitMQ |
| `PORT` | Puerto HTTP del servicio |
