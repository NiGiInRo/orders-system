package com.ordersystem.orderservice.domain.ports.in;

import com.ordersystem.orderservice.domain.model.Order;

// Puerto de entrada: define el contrato del caso de uso.
// El controller llama esto — no sabe que existe OrderService.
public interface CreateOrderUseCase {

    Order execute(Command command);

    // Record: transporta los datos de entrada del caso de uso.
    // Inmutable por diseño — nadie puede modificar el command una vez creado.
    record Command(String customerId, String productId, Integer quantity) {}
}   