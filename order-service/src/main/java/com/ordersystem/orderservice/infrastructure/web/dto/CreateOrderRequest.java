package com.ordersystem.orderservice.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Record inmutable: transporta los datos del HTTP request al controller.
// Las anotaciones de validación viven aquí — no en el dominio.
public record CreateOrderRequest(

        @NotBlank(message = "customerId is required")
        String customerId,

        @NotBlank(message = "productId is required")
        String productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {}