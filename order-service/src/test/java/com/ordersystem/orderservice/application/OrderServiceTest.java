package com.ordersystem.orderservice.application;

import com.ordersystem.orderservice.domain.exception.OrderNotFoundException;
import com.ordersystem.orderservice.domain.model.Order;
import com.ordersystem.orderservice.domain.model.OrderCreatedEvent;
import com.ordersystem.orderservice.domain.model.OrderStatus;
import com.ordersystem.orderservice.domain.ports.in.CreateOrderUseCase;
import com.ordersystem.orderservice.domain.ports.out.OrderEventPublisher;
import com.ordersystem.orderservice.domain.ports.out.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    // @InjectMocks inyecta ambos mocks en OrderService via constructor
    @Mock
    private OrderEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    // ─── createOrder ──────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldSaveOrderAndReturnIt() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        var savedOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.execute(command);

        assertThat(result).isEqualTo(savedOrder);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldCreateOrderWithPendingStatus() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.execute(command);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_shouldPassCorrectDataToRepository() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.execute(command);

        assertThat(result.getCustomerId()).isEqualTo("customer-1");
        assertThat(result.getProductId()).isEqualTo("product-5");
        assertThat(result.getQuantity()).isEqualTo(3);
    }

    @Test
    void createOrder_shouldPublishOrderCreatedEvent_withCorrectData() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        var savedOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.execute(command);

        // ArgumentCaptor intercepta el evento y el correlationId para inspeccionar sus datos
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher, times(1)).publishOrderCreated(eventCaptor.capture(), correlationIdCaptor.capture());

        OrderCreatedEvent published = eventCaptor.getValue();
        assertThat(published.orderId()).isEqualTo(savedOrder.getId());
        assertThat(published.customerId()).isEqualTo("customer-1");
        assertThat(published.productId()).isEqualTo("product-5");
        assertThat(published.quantity()).isEqualTo(3);

        // el correlationId debe existir y ser un UUID válido generado por orden
        assertThat(correlationIdCaptor.getValue()).isNotBlank();
    }

    @Test
    void createOrder_shouldGenerateDifferentCorrelationId_perOrder() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.execute(command);
        orderService.execute(command);

        ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher, times(2)).publishOrderCreated(any(), correlationIdCaptor.capture());

        var ids = correlationIdCaptor.getAllValues();
        // si esto falla, el correlationId volvió a quedar fijo por instancia del bean
        assertThat(ids.get(0)).isNotEqualTo(ids.get(1));
    }

    @Test
    void createOrder_shouldNotPublishEvent_whenSaveFails() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() -> orderService.execute(command))
                .isInstanceOf(RuntimeException.class);

        // si la persistencia falla, el evento no debe llegar al broker
        verify(eventPublisher, never()).publishOrderCreated(any(), any());
    }

    // ─── getOrder ─────────────────────────────────────────────────────────────

    @Test
    void getOrder_shouldReturnOrder_whenFound() {
        UUID id = UUID.randomUUID();
        Order existingOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existingOrder));

        Order result = orderService.execute(id);

        assertThat(result).isEqualTo(existingOrder);
    }

    @Test
    void getOrder_shouldThrowOrderNotFoundException_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.execute(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getOrder_shouldNotCallSave_whenFindingOrder() {
        UUID id = UUID.randomUUID();
        Order existingOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existingOrder));

        orderService.execute(id);

        verify(orderRepository, never()).save(any());
    }
}