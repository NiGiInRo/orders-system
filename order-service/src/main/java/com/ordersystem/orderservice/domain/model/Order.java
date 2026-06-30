package com.ordersystem.orderservice.domain.model;

import lombok.Getter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Order {

    private UUID id;
    private String customerId;
    private String productId;
    private Integer quantity;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor de negocio: el único punto de entrada para crear una orden nueva.
    // El dominio genera su propio UUID — no depende de que la DB lo asigne.
    public Order(String customerId, String productId, Integer quantity) {
        this.id = UUID.randomUUID();
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor de reconstitución: solo lo usa el mapper para reconstruir
    // una Order desde la DB. No es para lógica de negocio.
    public Order(UUID id, String customerId, String productId, Integer quantity,
                 OrderStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = OrderStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }
}