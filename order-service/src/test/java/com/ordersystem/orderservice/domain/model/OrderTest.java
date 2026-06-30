package com.ordersystem.orderservice.domain.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// Tests de dominio puro — sin Spring, sin DB, sin mocks.
// Cada test verifica una sola regla de negocio.
class OrderTest {

    // ─── Constructor de negocio ───────────────────────────────────────────────

    @Test
    void newOrder_shouldStartWithPendingStatus() {
        // Arrange + Act
        Order order = new Order("customer-1", "product-5", 3);

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void newOrder_shouldGenerateId() {
        Order order = new Order("customer-1", "product-5", 3);

        // El dominio genera su propio UUID — nunca debe ser null
        assertThat(order.getId()).isNotNull();
    }

    @Test
    void newOrder_shouldAssignTimestamps() {
        LocalDateTime before = LocalDateTime.now();
        Order order = new Order("customer-1", "product-5", 3);
        LocalDateTime after = LocalDateTime.now();

        // createdAt y updatedAt deben caer entre before y after
        assertThat(order.getCreatedAt()).isBetween(before, after);
        assertThat(order.getUpdatedAt()).isBetween(before, after);
    }

    @Test
    void newOrder_shouldPreserveInputData() {
        Order order = new Order("customer-1", "product-5", 3);

        assertThat(order.getCustomerId()).isEqualTo("customer-1");
        assertThat(order.getProductId()).isEqualTo("product-5");
        assertThat(order.getQuantity()).isEqualTo(3);
    }

    // ─── Transición confirm() ─────────────────────────────────────────────────

    @Test
    void confirm_shouldChangeStatusToConfirmed() {
        // Arrange
        Order order = new Order("customer-1", "product-5", 3);

        // Act
        order.confirm();

        // Assert
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void confirm_shouldUpdateUpdatedAt() throws InterruptedException {
        Order order = new Order("customer-1", "product-5", 3);
        LocalDateTime createdAt = order.getUpdatedAt();

        // Pequeña pausa para garantizar que updatedAt cambie
        Thread.sleep(10);
        order.confirm();

        // updatedAt debe ser posterior al momento de creación
        assertThat(order.getUpdatedAt()).isAfter(createdAt);
    }

    // ─── Transición reject() ──────────────────────────────────────────────────

    @Test
    void reject_shouldChangeStatusToRejected() {
        Order order = new Order("customer-1", "product-5", 3);

        order.reject();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void reject_shouldUpdateUpdatedAt() throws InterruptedException {
        Order order = new Order("customer-1", "product-5", 3);
        LocalDateTime createdAt = order.getUpdatedAt();

        Thread.sleep(10);
        order.reject();

        assertThat(order.getUpdatedAt()).isAfter(createdAt);
    }

    // ─── Constructor de reconstitución ───────────────────────────────────────

    @Test
    void reconstitution_shouldRestoreExactState() {
        // Arrange — valores que "vendrían de la DB"
        UUID existingId = UUID.randomUUID();
        LocalDateTime existingDate = LocalDateTime.of(2026, 1, 1, 10, 0);

        // Act — el mapper usaría este constructor al leer de DB
        Order order = new Order(
                existingId,
                "customer-1",
                "product-5",
                3,
                OrderStatus.CONFIRMED,
                existingDate,
                existingDate
        );

        // Assert — no genera nada nuevo, restaura exactamente lo que recibió
        assertThat(order.getId()).isEqualTo(existingId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getCreatedAt()).isEqualTo(existingDate);
    }
}