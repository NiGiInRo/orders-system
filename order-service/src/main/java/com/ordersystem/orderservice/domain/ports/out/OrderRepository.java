package com.ordersystem.orderservice.domain.ports.out;

import com.ordersystem.orderservice.domain.model.Order;
import java.util.Optional;
import java.util.UUID;

// Puerto de salida: el dominio declara QUÉ necesita de persistencia,
// sin saber CÓMO se implementa (JPA, MongoDB, en memoria, lo que sea).
// La implementación real vive en infrastructure/.
public interface OrderRepository {

    // Guarda una orden nueva o actualiza una existente.
    Order save(Order order);

    // Busca una orden por ID. Retorna Optional porque puede no existir.
    Optional<Order> findById(UUID id);
}