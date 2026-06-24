package com.ordersystem.orderservice.infrastructure.persistence;

import com.ordersystem.orderservice.domain.model.Order;
import org.springframework.stereotype.Component;

@Component  // Spring lo gestiona como bean para poder inyectarlo donde se necesite
public class OrderMapper {

    // Dominio → JPA: cuando vas a persistir una orden
    public OrderEntity toEntity(Order order) {
        return new OrderEntity(
                order.getId(),
                order.getCustomerId(),
                order.getProductId(),
                order.getQuantity(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    // JPA → Dominio: cuando lees de DB y necesitas un objeto de dominio
    // Usa el constructor de reconstitución — restaura el estado exacto, no crea uno nuevo
    public Order toDomain(OrderEntity entity) {
        return new Order(
                entity.getId(),
                entity.getCustomerId(),
                entity.getProductId(),
                entity.getQuantity(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}