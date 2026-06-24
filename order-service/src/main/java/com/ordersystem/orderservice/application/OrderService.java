package com.ordersystem.orderservice.application;

import com.ordersystem.orderservice.domain.exception.OrderNotFoundException;
import com.ordersystem.orderservice.domain.model.Order;
import com.ordersystem.orderservice.domain.ports.in.CreateOrderUseCase;
import com.ordersystem.orderservice.domain.ports.in.GetOrderUseCase;
import com.ordersystem.orderservice.domain.ports.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor  // inyecta OrderRepository por constructor, no por @Autowired
public class OrderService implements CreateOrderUseCase, GetOrderUseCase {

    // El service solo conoce el puerto — no sabe que existe JpaOrderRepositoryAdapter
    private final OrderRepository orderRepository;

    @Override
    public Order execute(CreateOrderUseCase.Command command) {
        // construye el objeto de dominio — el constructor asigna PENDING y genera UUID
        Order order = new Order(
                command.customerId(),
                command.productId(),
                command.quantity()
        );
        // delega la persistencia al puerto out — no sabe cómo se implementa
        return orderRepository.save(order);
    }

    @Override
    public Order execute(UUID id) {
        // orElseThrow: si el Optional está vacío, lanza la excepción de dominio
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}