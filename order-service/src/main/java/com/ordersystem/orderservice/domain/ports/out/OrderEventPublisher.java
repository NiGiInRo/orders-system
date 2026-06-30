package com.ordersystem.orderservice.domain.ports.out;

import com.ordersystem.orderservice.domain.model.OrderCreatedEvent;

// Puerto de salida — el dominio declara que necesita publicar eventos.
// No sabe si el adaptador usa RabbitMQ, Kafka, o cualquier otro mecanismo.
public interface OrderEventPublisher {
    void publishOrderCreated(OrderCreatedEvent event);
}
