package com.ordersystem.orderservice.domain.model;

// Los tres estados posibles del ciclo de vida de una orden.
// PENDING → estado inicial; CONFIRMED / REJECTED los asigna inventory-service via evento.
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED
}