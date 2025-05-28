package com.project_final.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_final.order_service.Dto.CreateOrderRequest;
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
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(2)))
                .andExpect(jsonPath("$.totalPrice", is(2599.98)))
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("POST /api/orders - Should return 400 when order creation fails")
    void createOrder_BadRequest() throws Exception {
        // Arrange
        when(orderService.createOrder(any(CreateOrderRequest.class)))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // Act & Assert
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

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
                .andExpect(jsonPath("$.status", is("DELIVERED")));

        verify(orderService).updateOrderStatus(1L, Order.OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status - Should return 400 for invalid status")
    void updateOrderStatus_InvalidStatus() throws Exception {
        // Act & Assert
        mockMvc.perform(put("/api/orders/1/status")
                        .param("status", "INVALID_STATUS"))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(any(), any());
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
                .andExpect(jsonPath("$.status", is("CANCELLED")));

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
                .andExpect(status().isBadRequest());

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
}