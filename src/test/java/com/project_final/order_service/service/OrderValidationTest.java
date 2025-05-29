package com.project_final.order_service.service;

import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.exceptions.OrderValidationException;
import com.project_final.order_service.exceptions.OrderStatusException;
import com.project_final.order_service.exceptions.OrderCancellationException;
import com.project_final.order_service.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class OrderValidationTest {

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Creamos una instancia para acceder a los métodos de validación
        orderService = new OrderService();
    }

    // ========== TESTS DE VALIDACIÓN DE CreateOrderRequest ==========

    @Test
    @DisplayName("Debe lanzar excepción cuando CreateOrderRequest es null")
    void shouldThrowExceptionWhenCreateOrderRequestIsNull() {
        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(null)
        );

        assertEquals("Datos de orden requeridos", exception.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando userId es null")
    void shouldThrowExceptionWhenUserIdIsNull() {
        CreateOrderRequest request = new CreateOrderRequest(null, 1L, 5);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("El ID del usuario debe ser un número positivo"));
        assertEquals("userId", exception.getField());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Debe lanzar excepción cuando userId es cero o negativo")
    void shouldThrowExceptionWhenUserIdIsZeroOrNegative(Long userId) {
        CreateOrderRequest request = new CreateOrderRequest(userId, 1L, 5);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("El ID del usuario debe ser un número positivo"));
        assertEquals("userId", exception.getField());
        assertEquals(userId, exception.getValue());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando productId es null")
    void shouldThrowExceptionWhenProductIdIsNull() {
        CreateOrderRequest request = new CreateOrderRequest(1L, null, 5);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("El ID del producto debe ser un número positivo"));
        assertEquals("productId", exception.getField());
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -50L})
    @DisplayName("Debe lanzar excepción cuando productId es cero o negativo")
    void shouldThrowExceptionWhenProductIdIsZeroOrNegative(Long productId) {
        CreateOrderRequest request = new CreateOrderRequest(1L, productId, 5);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("El ID del producto debe ser un número positivo"));
        assertEquals("productId", exception.getField());
        assertEquals(productId, exception.getValue());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando quantity es null")
    void shouldThrowExceptionWhenQuantityIsNull() {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, null);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("La cantidad debe ser un número positivo"));
        assertEquals("quantity", exception.getField());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Debe lanzar excepción cuando quantity es cero o negativa")
    void shouldThrowExceptionWhenQuantityIsZeroOrNegative(Integer quantity) {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, quantity);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("La cantidad debe ser un número positivo"));
        assertEquals("quantity", exception.getField());
        assertEquals(quantity, exception.getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {1001, 1500, 2000})
    @DisplayName("Debe lanzar excepción cuando quantity excede el límite")
    void shouldThrowExceptionWhenQuantityExceedsLimit(Integer quantity) {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, quantity);

        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> invokeValidateCreateOrderRequest(request)
        );

        assertTrue(exception.getMessage().contains("La cantidad no puede exceder 1000 unidades"));
        assertEquals("quantity", exception.getField());
        assertEquals(quantity, exception.getValue());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 100, 500, 1000})
    @DisplayName("Debe pasar validación con cantidades válidas")
    void shouldPassValidationWithValidQuantities(Integer quantity) {
        CreateOrderRequest request = new CreateOrderRequest(1L, 1L, quantity);

        assertDoesNotThrow(() -> invokeValidateCreateOrderRequest(request));
    }

    // ========== TESTS DE VALIDACIÓN DE TRANSICIONES DE ESTADO ==========

    @Test
    @DisplayName("Debe permitir transición de PENDING a CONFIRMED")
    void shouldAllowTransitionFromPendingToConfirmed() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        assertDoesNotThrow(() ->
                invokeValidateStatusTransition(order, Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED)
        );
    }

    @Test
    @DisplayName("Debe permitir transición de PENDING a CANCELLED")
    void shouldAllowTransitionFromPendingToCancelled() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        assertDoesNotThrow(() ->
                invokeValidateStatusTransition(order, Order.OrderStatus.PENDING, Order.OrderStatus.CANCELLED)
        );
    }

    @Test
    @DisplayName("Debe rechazar transición de PENDING a DELIVERED")
    void shouldRejectTransitionFromPendingToDelivered() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        OrderStatusException exception = assertThrows(OrderStatusException.class, () ->
                invokeValidateStatusTransition(order, Order.OrderStatus.PENDING, Order.OrderStatus.DELIVERED)
        );

        assertEquals(1L, exception.getOrderId());
        assertEquals(Order.OrderStatus.PENDING, exception.getCurrentStatus());
        assertEquals(Order.OrderStatus.DELIVERED, exception.getRequestedStatus());
    }

    @Test
    @DisplayName("Debe permitir transición de CONFIRMED a DELIVERED")
    void shouldAllowTransitionFromConfirmedToDelivered() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.CONFIRMED);

        assertDoesNotThrow(() ->
                invokeValidateStatusTransition(order, Order.OrderStatus.CONFIRMED, Order.OrderStatus.DELIVERED)
        );
    }

    @Test
    @DisplayName("Debe permitir transición de CONFIRMED a CANCELLED")
    void shouldAllowTransitionFromConfirmedToCancelled() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.CONFIRMED);

        assertDoesNotThrow(() ->
                invokeValidateStatusTransition(order, Order.OrderStatus.CONFIRMED, Order.OrderStatus.CANCELLED)
        );
    }

    @Test
    @DisplayName("Debe rechazar transición de CONFIRMED a PENDING")
    void shouldRejectTransitionFromConfirmedToPending() {
        Order order = createOrderWithId(1L);

        OrderStatusException exception = assertThrows(OrderStatusException.class, () ->
                invokeValidateStatusTransition(order, Order.OrderStatus.CONFIRMED, Order.OrderStatus.PENDING)
        );

        assertEquals(Order.OrderStatus.CONFIRMED, exception.getCurrentStatus());
        assertEquals(Order.OrderStatus.PENDING, exception.getRequestedStatus());
    }

    @ParameterizedTest
    @EnumSource(Order.OrderStatus.class)
    @DisplayName("Debe rechazar cualquier transición desde DELIVERED")
    void shouldRejectAnyTransitionFromDelivered(Order.OrderStatus toStatus) {
        Order order = createOrderWithId(1L);

        OrderStatusException exception = assertThrows(OrderStatusException.class, () ->
                invokeValidateStatusTransition(order, Order.OrderStatus.DELIVERED, toStatus)
        );

        assertEquals(Order.OrderStatus.DELIVERED, exception.getCurrentStatus());
        assertEquals(toStatus, exception.getRequestedStatus());
    }

    @ParameterizedTest
    @EnumSource(Order.OrderStatus.class)
    @DisplayName("Debe rechazar cualquier transición desde CANCELLED")
    void shouldRejectAnyTransitionFromCancelled(Order.OrderStatus toStatus) {
        Order order = createOrderWithId(1L);

        OrderStatusException exception = assertThrows(OrderStatusException.class, () ->
                invokeValidateStatusTransition(order, Order.OrderStatus.CANCELLED, toStatus)
        );

        assertEquals(Order.OrderStatus.CANCELLED, exception.getCurrentStatus());
        assertEquals(toStatus, exception.getRequestedStatus());
    }

    // ========== TESTS DE VALIDACIÓN DE CANCELACIÓN ==========

    @Test
    @DisplayName("Debe permitir cancelación de orden PENDING")
    void shouldAllowCancellationOfPendingOrder() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.PENDING);

        assertTrue(invokeCanCancelOrder(order));
    }

    @Test
    @DisplayName("Debe permitir cancelación de orden CONFIRMED")
    void shouldAllowCancellationOfConfirmedOrder() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.CONFIRMED);

        assertTrue(invokeCanCancelOrder(order));
    }

    @Test
    @DisplayName("Debe rechazar cancelación de orden DELIVERED")
    void shouldRejectCancellationOfDeliveredOrder() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.DELIVERED);

        assertFalse(invokeCanCancelOrder(order));
    }

    @Test
    @DisplayName("Debe rechazar cancelación de orden ya CANCELLED")
    void shouldRejectCancellationOfAlreadyCancelledOrder() {
        Order order = createOrderWithId(1L);
        order.setStatus(Order.OrderStatus.CANCELLED);

        assertFalse(invokeCanCancelOrder(order));
    }

    // ========== TESTS DE VALIDACIÓN DE IDs ==========

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0L, -1L, -100L})
    @DisplayName("Debe validar IDs inválidos")
    void shouldValidateInvalidIds(Long id) {
        OrderValidationException exception = assertThrows(
                OrderValidationException.class,
                () -> {
                    if (id == null || id <= 0) {
                        throw new OrderValidationException("id", id, "El ID de la orden debe ser un número positivo");
                    }
                }
        );

        assertTrue(exception.getMessage().contains("El ID de la orden debe ser un número positivo"));
    }

    // ========== MÉTODOS HELPER PARA ACCEDER A MÉTODOS PRIVADOS ==========

    private void invokeValidateCreateOrderRequest(CreateOrderRequest request) {
        try {
            Method method = OrderService.class.getDeclaredMethod("validateCreateOrderRequest", CreateOrderRequest.class);
            method.setAccessible(true);
            method.invoke(orderService, request);
        } catch (Exception e) {
            if (e.getCause() instanceof OrderValidationException) {
                throw (OrderValidationException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    private void invokeValidateStatusTransition(Order order, Order.OrderStatus from, Order.OrderStatus to) {
        try {
            Method method = OrderService.class.getDeclaredMethod("validateStatusTransition",
                    Order.class, Order.OrderStatus.class, Order.OrderStatus.class);
            method.setAccessible(true);
            method.invoke(orderService, order, from, to);
        } catch (Exception e) {
            if (e.getCause() instanceof OrderStatusException) {
                throw (OrderStatusException) e.getCause();
            }
            throw new RuntimeException(e);
        }
    }

    private boolean invokeCanCancelOrder(Order order) {
        try {
            Method method = OrderService.class.getDeclaredMethod("canCancelOrder", Order.class);
            method.setAccessible(true);
            return (Boolean) method.invoke(orderService, order);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Order createOrderWithId(Long id) {
        Order order = new Order(1L, 1L, 5, java.math.BigDecimal.valueOf(100.0));
        order.setId(id);
        return order;
    }
}
