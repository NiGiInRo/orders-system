package com.ordersystem.orderservice.infrastructure.web.dto;

import com.ordersystem.orderservice.domain.model.Order;
import java.time.LocalDateTime;
import java.util.UUID;

// Lo que el cliente recibe como respuesta. 
// Tiene un factory method estático para construirse desde un objeto de dominio.
public record OrderResponse(
        UUID id,
        String customerId,
        String productId,
        Integer quantity,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // Convierte Order (dominio) → OrderResponse (HTTP).
    // El controller llama esto — no manipula Order directamente.
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getStatus().name(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}