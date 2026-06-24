package com.ordersystem.orderservice.application;

import com.ordersystem.orderservice.domain.exception.OrderNotFoundException;
import com.ordersystem.orderservice.domain.model.Order;
import com.ordersystem.orderservice.domain.model.OrderStatus;
import com.ordersystem.orderservice.domain.ports.in.CreateOrderUseCase;
import com.ordersystem.orderservice.domain.ports.out.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class) activa Mockito sin levantar Spring.
// Los tests corren en milisegundos — no hay contexto, no hay DB, no hay red.
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    // @Mock crea un doble del repositorio — no toca la DB real.
    // Vos controlás qué devuelve cada método.
    @Mock
    private OrderRepository orderRepository;

    // @InjectMocks crea el OrderService e inyecta los @Mock declarados arriba.
    @InjectMocks
    private OrderService orderService;

    // ─── createOrder ──────────────────────────────────────────────────────────

    @Test
    void createOrder_shouldSaveOrderAndReturnIt() {
        // Arrange — definís qué devuelve el mock cuando se llame save()
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        var savedOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        // Act
        Order result = orderService.execute(command);

        // Assert — el resultado es la orden que devolvió el repositorio
        assertThat(result).isEqualTo(savedOrder);
        // verify confirma que save() fue llamado exactamente una vez
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldCreateOrderWithPendingStatus() {
        // Arrange
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        // capturamos el Order que se pasa a save() para inspeccionarlo
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.execute(command);

        // Assert — el dominio siempre crea la orden en PENDING
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void createOrder_shouldPassCorrectDataToRepository() {
        var command = new CreateOrderUseCase.Command("customer-1", "product-5", 3);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order result = orderService.execute(command);

        // Los datos del command deben llegar intactos al objeto persistido
        assertThat(result.getCustomerId()).isEqualTo("customer-1");
        assertThat(result.getProductId()).isEqualTo("product-5");
        assertThat(result.getQuantity()).isEqualTo(3);
    }

    // ─── getOrder ─────────────────────────────────────────────────────────────

    @Test
    void getOrder_shouldReturnOrder_whenFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        Order existingOrder = new Order("customer-1", "product-5", 3);
        when(orderRepository.findById(id)).thenReturn(Optional.of(existingOrder));

        // Act
        Order result = orderService.execute(id);

        // Assert
        assertThat(result).isEqualTo(existingOrder);
    }

    @Test
    void getOrder_shouldThrowOrderNotFoundException_whenNotFound() {
        // Arrange — el mock devuelve Optional vacío (orden no existe)
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        // Assert — assertThatThrownBy verifica que se lanza la excepción correcta
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

        // Verificamos que una consulta nunca modifica datos
        verify(orderRepository, never()).save(any());
    }
}