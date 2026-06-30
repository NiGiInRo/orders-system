package com.ordersystem.orderservice.domain.exception;

import java.util.UUID;

// Excepción de dominio: el negocio no encontró la orden solicitada.
// Extiende RuntimeException — no obliga a quien llama a hacer try/catch.
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID id) {
        super("Order with id " + id + " not found");
    }
}