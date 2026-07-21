package com.ordersystem.orderservice.application;

import com.ordersystem.orderservice.domain.exception.OrderNotFoundException;
import com.ordersystem.orderservice.domain.model.Order;
import com.ordersystem.orderservice.domain.model.OrderCreatedEvent;
import com.ordersystem.orderservice.domain.ports.in.CreateOrderUseCase;
import com.ordersystem.orderservice.domain.ports.in.GetOrderUseCase;
import com.ordersystem.orderservice.domain.ports.out.OrderEventPublisher;
import com.ordersystem.orderservice.domain.ports.out.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor  // inyecta OrderRepository por constructor, no por @Autowired
public class OrderService implements CreateOrderUseCase, GetOrderUseCase {

    // El service solo conoce el puerto — no sabe que existe JpaOrderRepositoryAdapter
    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Override
    public Order execute(CreateOrderUseCase.Command command) {
        // construye el objeto de dominio — el constructor asigna PENDING y genera UUID
        Order order = new Order(
                command.customerId(),
                command.productId(),
                command.quantity()
        );
        // primero persistir — si falla, el evento nunca se publica
        Order saved = orderRepository.save(order);

        // correlationId por orden, no por instancia del bean — cada creación
        // de orden necesita su propio identificador de trazabilidad
        String correlationId = UUID.randomUUID().toString();

        // luego publicar — el evento lleva los datos de la orden ya guardada
        eventPublisher.publishOrderCreated(
                new OrderCreatedEvent(saved.getId(), saved.getCustomerId(),
                        saved.getProductId(), saved.getQuantity()),
                correlationId
        );

        return saved;
    }

    @Override
    public Order execute(UUID id) {
        // orElseThrow: si el Optional está vacío, lanza la excepción de dominio
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }
}