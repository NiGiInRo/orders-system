package com.ordersystem.orderservice.infrastructure.persistence;

import com.ordersystem.orderservice.domain.model.Order;
import com.ordersystem.orderservice.domain.ports.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor  // Lombok genera constructor con los campos final — patrón de inyección recomendado
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringDataOrderRepository springDataRepo;
    private final OrderMapper mapper;

    @Override
    public Order save(Order order) {
        OrderEntity entity = mapper.toEntity(order);        // dominio → JPA
        OrderEntity saved = springDataRepo.save(entity);    // JPA persiste en DB
        return mapper.toDomain(saved);                      // JPA → dominio para retornar
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return springDataRepo.findById(id)   // busca el entity en DB
                .map(mapper::toDomain);      // si existe, lo convierte a dominio
    }
}