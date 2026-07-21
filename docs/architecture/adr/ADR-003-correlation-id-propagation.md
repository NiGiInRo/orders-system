# ADR-003 — Propagación de Correlation ID vía propiedad nativa de AMQP

## Estado
Aceptado

## Contexto
La Fase 3 (Notification Service) exige que cada log registrado tras consumir
`inventory.checked` incluya un `correlationId` que permita rastrear una orden
a lo largo de los tres eventos del flujo (`order.created` → `inventory.checked`
→ log de notificación).

Al revisar el código de `order-service` e `inventory-service` (Fases 1 y 2,
ya mergeadas a `main`) se confirmó que no existe ningún mecanismo de
correlación: ni en el dominio, ni en el payload de los eventos, ni en las
propiedades de los mensajes de RabbitMQ. Para que `notification-service`
pueda loguear un `correlationId` realmente trazable de punta a punta, ese
identificador debe nacer en el punto de entrada del flujo — la creación de
la orden — y viajar sin cortes ni regeneraciones por los tres servicios.

Se evaluaron dos formas de transportarlo:

1. **Propiedad nativa de mensaje AMQP** (`correlation_id`, parte del
   protocolo AMQP 0-9-1 desde su especificación original). Soportada de
   fábrica por Spring AMQP (`MessageProperties`), `aio-pika` (Python) y
   `amqplib` (Node) — no requiere ninguna librería adicional.
2. **Campo `correlationId` embebido en el JSON del body del evento**, junto
   a `orderId`, `customerId`, etc.

## Decisión
Se adopta la opción 1: el `correlationId` viaja como propiedad nativa del
mensaje AMQP, nunca como campo del payload JSON.

- Se genera un `UUID` en `OrderService.execute(CreateOrderUseCase.Command)`
  en el momento de crear la orden — es el punto de entrada real del flujo
  en el estado actual del sistema (sin API Gateway todavía).
- El puerto de salida `OrderEventPublisher.publishOrderCreated` recibe el
  `correlationId` como parámetro separado del `OrderCreatedEvent`. El
  evento de dominio conserva únicamente datos de negocio — el
  `correlationId` es un dato de observabilidad, no un hecho de dominio.
- El adaptador `RabbitMqOrderEventPublisher` escribe el `correlationId` en
  `MessageProperties` (vía `MessagePostProcessor`) antes de publicar, sin
  tocar el body JSON del evento.
- `inventory-service` lee el `correlationId` de las propiedades del mensaje
  `order.created` entrante y lo reescribe, sin transformarlo, en las
  propiedades del mensaje `inventory.checked` que publica.
- `notification-service` lee el `correlationId` de las propiedades AMQP del
  mensaje `inventory.checked` (no del body) para construir el log
  estructurado exigido por HU-17.

## Consecuencias

**Positivas:**
- El `correlationId` no contamina los contratos JSON de los eventos de
  negocio — sigue siendo un dato de trazabilidad, no un campo que las
  reglas de negocio deban conocer o validar.
- Aprovecha una capacidad nativa del protocolo AMQP en lugar de
  reinventarla con un campo custom, coherente con el objetivo del proyecto
  de profundizar en RabbitMQ.
- El mismo mecanismo aplica igual en los tres lenguajes (Java/Spring AMQP,
  Python/aio-pika, Node/amqplib) sin lógica adicional de serialización.

**Negativas / trade-offs:**
- Requiere reabrir `order-service` e `inventory-service`, ya mergeados a
  `main` — implica ramas de feature adicionales sobre código de fases ya
  cerradas.
- El `correlationId` no es visible en el body del mensaje al inspeccionarlo
  en RabbitMQ Management UI — solo aparece en la pestaña de propiedades del
  mensaje, ligeramente menos cómodo para debugging manual que si fuera un
  campo del JSON.
- Mientras no exista API Gateway (Fase 4), el `correlationId` siempre se
  genera en `order-service`; no hay propagación desde un header HTTP de
  entrada.

**Decisión de revisión:** al construir el API Gateway en Fase 4, evaluar si
el `correlationId` debe originarse ahí (por ejemplo, desde un header
`X-Correlation-Id` generado en el borde del sistema y propagado por HTTP
hasta `order-service`) en lugar de generarse internamente en
`OrderService`.
