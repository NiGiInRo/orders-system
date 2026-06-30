package com.ordersystem.orderservice.domain.ports.in;

import com.ordersystem.orderservice.domain.model.Order;
import java.util.UUID;

// Puerto de entrada para consulta.
// Separado de CreateOrderUseCase — cada interfaz tiene una sola responsabilidad.
public interface GetOrderUseCase {

    Order execute(UUID id);
}