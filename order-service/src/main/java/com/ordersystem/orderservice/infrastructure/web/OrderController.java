package com.ordersystem.orderservice.infrastructure.web;

import com.ordersystem.orderservice.domain.ports.in.CreateOrderUseCase;
import com.ordersystem.orderservice.domain.ports.in.GetOrderUseCase;
import com.ordersystem.orderservice.infrastructure.web.dto.CreateOrderRequest;
import com.ordersystem.orderservice.infrastructure.web.dto.OrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    // El controller depende de los puertos IN — no sabe que existe OrderService
    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)  // retorna 201 en vez del 200 por defecto
    public OrderResponse createOrder(@Valid @RequestBody CreateOrderRequest request) {
        // @Valid activa las validaciones del record antes de llegar al caso de uso
        var order = createOrderUseCase.execute(
                new CreateOrderUseCase.Command(
                        request.customerId(),
                        request.productId(),
                        request.quantity()
                )
        );
        return OrderResponse.from(order);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        var order = getOrderUseCase.execute(id);
        return OrderResponse.from(order);
    }
}