package com.ordersystem.orderservice.domain.model;

import java.util.UUID;

// Representa el hecho de que una orden fue creada.
// Inmutable por diseño — los eventos no se modifican, solo se publican.
public record OrderCreatedEvent(
        UUID orderId,
        String customerId,
        String productId,
        int quantity
) {}