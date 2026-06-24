package com.ordersystem.orderservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

// Spring Data genera el SQL de save/findById/etc. en tiempo de arranque.
// Trabaja con OrderEntity, no con Order — no sabe nada del dominio.
public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {
}