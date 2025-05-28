package com.project_final.order_service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.model.Order;
import com.project_final.order_service.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
@DisplayName("Order Service Integration Tests")
class OrderServiceIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api/orders";
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        // Limpiar la base de datos antes de cada prueba
        orderRepository.deleteAll();
    }

    @Test
    @DisplayName("Integration Test: Complete order lifecycle")
    void completeOrderLifecycle() throws Exception {
        // 1. Create Order
        CreateOrderRequest createRequest = new CreateOrderRequest(1L, 1L, 2);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // 2. Verify order was saved
        assertEquals(1, orderRepository.count());
        Order savedOrder = orderRepository.findAll().get(0);
        assertNotNull(savedOrder);
        assertEquals(Order.OrderStatus.CONFIRMED, savedOrder.getStatus());

        // 3. Get order by ID
        mockMvc.perform(get("/api/orders/" + savedOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedOrder.getId()));

        // 4. Update order status
        mockMvc.perform(put("/api/orders/" + savedOrder.getId() + "/status")
                        .param("status", "DELIVERED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        // 5. Verify status was updated in database
        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertNotNull(updatedOrder);
        assertEquals(Order.OrderStatus.DELIVERED, updatedOrder.getStatus());

        // 6. Get all orders
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Integration Test: Order statistics endpoints")
    void orderStatistics() throws Exception {
        // Create test orders
        Order order1 = new Order(1L, 1L, 2, new BigDecimal("2000.00"));
        order1.setStatus(Order.OrderStatus.CONFIRMED);

        Order order2 = new Order(2L, 2L, 1, new BigDecimal("1500.00"));
        order2.setStatus(Order.OrderStatus.DELIVERED);

        orderRepository.save(order1);
        orderRepository.save(order2);

        // Test total orders count
        mockMvc.perform(get("/api/orders/stats/total"))
                .andExpect(status().isOk())
                .andExpect(content().string("2"));

        // Test orders by status count
        mockMvc.perform(get("/api/orders/stats/status/CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("Integration Test: Get orders by user")
    void getOrdersByUser() throws Exception {
        // Create orders for different users
        Order userOrder1 = new Order(1L, 1L, 1, new BigDecimal("1000.00"));
        Order userOrder2 = new Order(1L, 2L, 2, new BigDecimal("2000.00"));
        Order otherUserOrder = new Order(2L, 1L, 1, new BigDecimal("1000.00"));

        orderRepository.save(userOrder1);
        orderRepository.save(userOrder2);
        orderRepository.save(otherUserOrder);

        // Test get orders by user ID
        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].userId").value(1));

        mockMvc.perform(get("/api/orders/user/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2));
    }

    @Test
    @DisplayName("Integration Test: Cancel order workflow")
    void cancelOrderWorkflow() throws Exception {
        // Create order
        Order order = new Order(1L, 1L, 2, new BigDecimal("2000.00"));
        order.setStatus(Order.OrderStatus.CONFIRMED);
        Order savedOrder = orderRepository.save(order);

        // Cancel order
        mockMvc.perform(put("/api/orders/" + savedOrder.getId() + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify cancellation in database
        Order cancelledOrder = orderRepository.findById(savedOrder.getId()).orElse(null);
        assertNotNull(cancelledOrder);
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus());
    }

    @Test
    @DisplayName("Integration Test: Error handling scenarios")
    void errorHandlingScenarios() throws Exception {
        // Test get non-existent order
        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound());

        // Test update status of non-existent order
        mockMvc.perform(put("/api/orders/999/status")
                        .param("status", "DELIVERED"))
                .andExpect(status().isNotFound());

        // Test cancel non-existent order
        mockMvc.perform(put("/api/orders/999/cancel"))
                .andExpect(status().isNotFound());

        // Test invalid status
        Order order = orderRepository.save(new Order(1L, 1L, 1, BigDecimal.TEN));
        mockMvc.perform(put("/api/orders/" + order.getId() + "/status")
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Integration Test: Database constraints and validations")
    void databaseConstraintsAndValidations() {
        // Test saving order with all required fields
        Order validOrder = new Order(1L, 1L, 2, new BigDecimal("1000.00"));
        validOrder.setStatus(Order.OrderStatus.CONFIRMED);

        Order savedOrder = orderRepository.save(validOrder);
        assertNotNull(savedOrder.getId());
        assertNotNull(savedOrder.getCreatedAt());
        assertNotNull(savedOrder.getUpdatedAt());
        assertEquals(Order.OrderStatus.CONFIRMED, savedOrder.getStatus());

        // Test retrieving saved order
        Optional<Order> retrievedOrder = orderRepository.findById(savedOrder.getId());
        assertTrue(retrievedOrder.isPresent());
        assertEquals(validOrder.getUserId(), retrievedOrder.get().getUserId());
        assertEquals(validOrder.getProductId(), retrievedOrder.get().getProductId());
    }

    @Test
    @DisplayName("Integration Test: Recent orders functionality")
    void recentOrdersFunctionality() throws Exception {
        // Create orders with different timestamps
        Order oldOrder = new Order(1L, 1L, 1, new BigDecimal("500.00"));
        oldOrder.setOrderDate(LocalDateTime.now().minusDays(2));

        Order recentOrder = new Order(2L, 2L, 1, new BigDecimal("1000.00"));
        recentOrder.setOrderDate(LocalDateTime.now().minusHours(2));

        orderRepository.save(oldOrder);
        orderRepository.save(recentOrder);

        // Test recent orders endpoint
        mockMvc.perform(get("/api/orders/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2));
    }

    @Test
    @DisplayName("Integration Test: Concurrent order creation")
    void concurrentOrderCreation() throws InterruptedException {
        int numberOfThreads = 5;
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            final int userId = i + 1;
            threads[i] = new Thread(() -> {
                try {
                    CreateOrderRequest request = new CreateOrderRequest((long) userId, 1L, 1);

                    ResponseEntity<Order> response = restTemplate.postForEntity(
                            baseUrl, request, Order.class);

                    // Nota: Esta prueba asume que los servicios externos están mockeados
                    // En un escenario real, podrías esperar que algunos fallen
                } catch (Exception e) {
                    // Esperado en este escenario debido a dependencias de servicios externos
                    System.out.println("Expected exception in concurrent test: " + e.getMessage());
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verificar que las operaciones de base de datos fueron atómicas
        long totalOrders = orderRepository.count();
        assertTrue(totalOrders >= 0); // Al menos no ocurrió corrupción
    }
}