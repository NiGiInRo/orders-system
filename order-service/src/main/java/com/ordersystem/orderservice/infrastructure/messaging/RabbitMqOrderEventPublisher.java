package com.ordersystem.orderservice.infrastructure.messaging;

import com.ordersystem.orderservice.domain.model.OrderCreatedEvent;
import com.ordersystem.orderservice.domain.ports.out.OrderEventPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Adaptador de salida — única clase que conoce RabbitMQ.
// El dominio solo ve la interfaz OrderEventPublisher.
@Component
public class RabbitMqOrderEventPublisher implements OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitMqOrderEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbitmq.exchange}") String exchange,
            @Value("${app.rabbitmq.routing-key}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event, String correlationId) {
        // convertAndSend serializa el evento a JSON y lo envía al exchange con el routing key.
        // El MessagePostProcessor inyecta el correlationId en las MessageProperties (AMQP),
        // sin tocar el body — no confundir con CorrelationData, que es para publisher confirms.
        rabbitTemplate.convertAndSend(exchange, routingKey, event, message -> {
            message.getMessageProperties().setCorrelationId(correlationId);
            return message;
        });
    }
}