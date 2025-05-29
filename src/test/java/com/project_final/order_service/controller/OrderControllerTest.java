package com.project_final.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.exceptions.*;
import com.project_final.order_service.model.Order;
import com.project_final.order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@DisplayName("Order Controller Tests")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    private Order testOrder;
    private CreateOrderRequest createRequest;

    @BeforeEach
    void setUp() {
        testOrder = new Order(1L, 1L, 2, new BigDecimal("2599.98"));
        testOrder.setId(1L);
        testOrder.setStatus(Order.OrderStatus.CONFIRMED);
        testOrder.setOrderDate(LocalDateTime.now());
        testOrder.setCreatedAt(LocalDateTime.now());
        testOrder.setUpdatedAt(LocalDateTime.now());

        createRequest = new CreateOrderRequest(1L, 1L, 2);
    }

    @Test
    @DisplayName("POST /api/orders - Should create order successfully")
    void createOrder_Success() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.userId", is(1)))
                .andExpect(jsonPath("$.data.productId", is(1)))
                .andExpect(jsonPath("$.data.quantity", is(2)))
                .andExpect(jsonPath("$.data.totalPrice", is(2599.98)))
                .andExpect(jsonPath("$.data.status", is("CONFIRMED")));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 503 when user service fails")
    void createOrder_UserServiceError() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new UserServiceException(1L, "validateUser", "Usuario no encontrado"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("EXTERNAL_SERVICE_ERROR")))
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.additionalInfo.userId", is(1)))
                .andExpect(jsonPath("$.additionalInfo.operation", is("validateUser")));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 503 when product service fails")
    void createOrder_ProductServiceError() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new ProductServiceException(1L, "validateProduct", "Producto no encontrado"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("EXTERNAL_SERVICE_ERROR")))
                .andExpect(jsonPath("$.status", is(503)))
                .andExpect(jsonPath("$.additionalInfo.productId", is(1)))
                .andExpect(jsonPath("$.additionalInfo.operation", is("validateProduct")));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 409 when insufficient stock")
    void createOrder_InsufficientStock() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new InsufficientStockException(1L, 5, 2));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("INSUFFICIENT_STOCK")))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.additionalInfo.productId", is(1)))
                .andExpect(jsonPath("$.additionalInfo.requestedQuantity", is(5)))
                .andExpect(jsonPath("$.additionalInfo.availableStock", is(2)));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 when validation fails")
    void createOrder_ValidationError() throws Exception {
        // Arrange
        CreateOrderRequest invalidRequest = new CreateOrderRequest(null, 1L, 2);
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new OrderValidationException("userId", null, "El ID del usuario es requerido"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.field", is("userId")));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("GET /api/orders - Should return all orders")
    void getAllOrders_Success() throws Exception {
        // Arrange
        Order order2 = new Order(2L, 2L, 1, new BigDecimal("1299.99"));
        order2.setId(2L);
        List<Order> orders = Arrays.asList(testOrder, order2);

        when(orderService.getAllOrders()).thenReturn(orders);

        // Act & Assert
        mockMvc.perform(get("/api/orders"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(orderService).getAllOrders();
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return order by ID")
    void getOrderById_Success() throws Exception {
        // Arrange
        when(orderService.getOrderById(1L)).thenReturn(Optional.of(testOrder));

        // Act & Assert
        mockMvc.perform(get("/api/orders/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.productId", is(1)));

        verify(orderService).getOrderById(1L);
    }

    @Test
    @DisplayName("GET /api/orders/{id} - Should return 404 when order not found")
    void getOrderById_NotFound() throws Exception {
        // Arrange
        when(orderService.getOrderById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/orders/999"))
                .andDo(print())
                .andExpect(status().isNotFound());

        verify(orderService).getOrderById(999L);
    }

    @Test
    @DisplayName("GET /api/orders/user/{userId} - Should return orders by user ID")
    void getOrdersByUserId_Success() throws Exception {
        // Arrange
        List<Order> userOrders = Arrays.asList(testOrder);
        when(orderService.getOrdersByUserId(1L)).thenReturn(userOrders);

        // Act & Assert
        mockMvc.perform(get("/api/orders/user/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(1)));

        verify(orderService).getOrdersByUserId(1L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should update order status")
    void updateOrderStatus_Success() throws Exception {
        // Arrange
        testOrder.setStatus(Order.OrderStatus.DELIVERED);
        when(orderService.updateOrderStatus(eq(1L), eq(Order.OrderStatus.DELIVERED)))
                .thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(put("/api/orders/1/status")
                        .param("status", "DELIVERED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.status", is("DELIVERED")));

        verify(orderService).updateOrderStatus(1L, Order.OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 400 for invalid status")
    void updateOrderStatus_InvalidStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/orders/1/status")
                        .param("status", "INVALID_STATUS"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.field", is("status")));

        verify(orderService, never()).updateOrderStatus(any(), any());
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 404 when order not found")
    void updateOrderStatus_OrderNotFound() throws Exception {
        // Arrange
        when(orderService.updateOrderStatus(eq(999L), eq(Order.OrderStatus.DELIVERED)))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(put("/api/orders/999/status")
                        .param("status", "DELIVERED"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)));

        verify(orderService).updateOrderStatus(999L, Order.OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 400 for invalid status transition")
    void updateOrderStatus_InvalidTransition() throws Exception {
        // Arrange
        when(orderService.updateOrderStatus(eq(1L), eq(Order.OrderStatus.PENDING)))
                .thenThrow(new OrderStatusException(1L, Order.OrderStatus.DELIVERED, Order.OrderStatus.PENDING));

        // Act & Assert
        mockMvc.perform(put("/api/orders/1/status")
                        .param("status", "PENDING"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_STATUS_ERROR")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(orderService).updateOrderStatus(1L, Order.OrderStatus.PENDING);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/cancel - Should cancel order")
    void cancelOrder_Success() throws Exception {
        // Arrange
        testOrder.setStatus(Order.OrderStatus.CANCELLED);
        when(orderService.cancelOrder(1L)).thenReturn(testOrder);

        // Act & Assert
        mockMvc.perform(put("/api/orders/1/cancel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.status", is("CANCELLED")));

        verify(orderService).cancelOrder(1L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/cancel - Should return 404 when order not found")
    void cancelOrder_OrderNotFound() throws Exception {
        // Arrange
        when(orderService.cancelOrder(999L))
                .thenThrow(new OrderNotFoundException(999L));

        // Act & Assert
        mockMvc.perform(put("/api/orders/999/cancel"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)));

        verify(orderService).cancelOrder(999L);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/cancel - Should return 400 when order cannot be cancelled")
    void cancelOrder_CannotCancel() throws Exception {
        // Arrange
        when(orderService.cancelOrder(1L))
                .thenThrow(new OrderCancellationException(1L, Order.OrderStatus.DELIVERED));

        // Act & Assert
        mockMvc.perform(put("/api/orders/1/cancel"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_CANCELLATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(orderService).cancelOrder(1L);
    }

    @Test
    @DisplayName("GET /api/orders/stats/total - Should return total orders count")
    void getTotalOrders_Success() throws Exception {
        // Arrange
        when(orderService.getTotalOrders()).thenReturn(10L);

        // Act & Assert
        mockMvc.perform(get("/api/orders/stats/total"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("10"));

        verify(orderService).getTotalOrders();
    }

    @Test
    @DisplayName("GET /api/orders/stats/sales - Should return total sales amount")
    void getTotalSales_Success() throws Exception {
        // Arrange
        when(orderService.getTotalSales()).thenReturn(new BigDecimal("15000.50"));

        // Act & Assert
        mockMvc.perform(get("/api/orders/stats/sales"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("15000.50"));

        verify(orderService).getTotalSales();
    }

    @Test
    @DisplayName("POST /api/orders - Should handle malformed JSON")
    void createOrder_MalformedJson() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.status", is(400)));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("GET /api/orders/recent - Should return recent orders")
    void getRecentOrders_Success() throws Exception {
        // Arrange
        List<Order> recentOrders = Arrays.asList(testOrder);
        when(orderService.getRecentOrders()).thenReturn(recentOrders);

        // Act & Assert
        mockMvc.perform(get("/api/orders/recent"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)));

        verify(orderService).getRecentOrders();
    }

    @Test
    @DisplayName("GET /api/orders/status/{status} - Should return orders by status")
    void getOrdersByStatus_Success() throws Exception {
        // Arrange
        List<Order> pendingOrders = Arrays.asList(testOrder);
        when(orderService.getOrdersByStatus(eq(Order.OrderStatus.CONFIRMED))).thenReturn(pendingOrders);

        // Act & Assert
        mockMvc.perform(get("/api/orders/status/CONFIRMED"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("CONFIRMED")));

        verify(orderService).getOrdersByStatus(eq(Order.OrderStatus.CONFIRMED));
    }

    @Test
    @DisplayName("GET /api/orders/status/{status} - Should return 400 for invalid status")
    void getOrdersByStatus_InvalidStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/orders/status/INVALID_STATUS"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode", is("ORDER_VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.field", is("status")));

        verify(orderService, never()).getOrdersByStatus(any(Order.OrderStatus.class));
    }
}