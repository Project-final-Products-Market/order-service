package com.project_final.order_service.service;

import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.Dto.ProductDto;
import com.project_final.order_service.Dto.UserDto;
import com.project_final.order_service.model.Order;
import com.project_final.order_service.repositories.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Order Service Tests")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderRequest validRequest;
    private UserDto validUser;
    private ProductDto validProduct;
    private Order validOrder;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = new CreateOrderRequest(1L, 1L, 2);

        validUser = new UserDto(1L, "John Doe", "john@example.com");

        validProduct = new ProductDto(1L, "Laptop", "Gaming Laptop",
                new BigDecimal("1299.99"), 10);

        validOrder = new Order(1L, 1L, 2, new BigDecimal("2599.98"));
        validOrder.setId(1L);
        validOrder.setStatus(Order.OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should create order successfully when all validations pass")
    void createOrder_Success() {
        // Arrange
        when(restTemplate.getForObject(contains("/users/1"), eq(UserDto.class)))
                .thenReturn(validUser);
        when(restTemplate.getForObject(contains("/products/1"), eq(ProductDto.class)))
                .thenReturn(validProduct);
        when(restTemplate.getForObject(contains("check-stock"), eq(Boolean.class)))
                .thenReturn(true);
        when(orderRepository.save(any(Order.class))).thenReturn(validOrder);

        // Act
        Order result = orderService.createOrder(validRequest);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(1L, result.getProductId());
        assertEquals(2, result.getQuantity());
        assertEquals(new BigDecimal("2599.98"), result.getTotalPrice());
        assertEquals(Order.OrderStatus.CONFIRMED, result.getStatus());

        // Verify interactions
        verify(orderRepository).save(any(Order.class));
        verify(restTemplate, times(3)).getForObject(anyString(), any(Class.class));
    }


    @Test
    @DisplayName("Should throw exception when insufficient stock")
    void createOrder_InsufficientStock() {
        // Arrange
        when(restTemplate.getForObject(contains("/users/1"), eq(UserDto.class)))
                .thenReturn(validUser);
        when(restTemplate.getForObject(contains("/products/1"), eq(ProductDto.class)))
                .thenReturn(validProduct);
        when(restTemplate.getForObject(contains("check-stock"), eq(Boolean.class)))
                .thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> orderService.createOrder(validRequest));

        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("Should get all orders successfully")
    void getAllOrders_Success() {
        // Arrange
        List<Order> expectedOrders = Arrays.asList(validOrder, new Order());
        when(orderRepository.findAll()).thenReturn(expectedOrders);

        // Act
        List<Order> result = orderService.getAllOrders();

        // Assert
        assertEquals(2, result.size());
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void getOrderById_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(validOrder));

        // Act
        Optional<Order> result = orderService.getOrderById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(validOrder.getId(), result.get().getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("Should update order status successfully")
    void updateOrderStatus_Success() {
        // Arrange
        when(orderRepository.findById(1L)).thenReturn(Optional.of(validOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(validOrder);

        // Act
        Order result = orderService.updateOrderStatus(1L, Order.OrderStatus.DELIVERED);

        // Assert
        assertEquals(Order.OrderStatus.DELIVERED, result.getStatus());
        verify(orderRepository).findById(1L);
        verify(orderRepository).save(validOrder);
    }


    @Test
    @DisplayName("Should get orders by user ID")
    void getOrdersByUserId_Success() {
        // Arrange
        List<Order> expectedOrders = Arrays.asList(validOrder);
        when(orderRepository.findByUserIdOrderByOrderDateDesc(1L))
                .thenReturn(expectedOrders);

        // Act
        List<Order> result = orderService.getOrdersByUserId(1L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getUserId());
        verify(orderRepository).findByUserIdOrderByOrderDateDesc(1L);
    }

    @Test
    @DisplayName("Should get total orders count")
    void getTotalOrders_Success() {
        // Arrange
        when(orderRepository.count()).thenReturn(5L);

        // Act
        Long result = orderService.getTotalOrders();

        // Assert
        assertEquals(5L, result);
        verify(orderRepository).count();
    }

    @Test
    @DisplayName("Should get total sales amount")
    void getTotalSales_Success() {
        // Arrange
        BigDecimal expectedTotal = new BigDecimal("5000.00");
        when(orderRepository.getTotalSales()).thenReturn(expectedTotal);

        // Act
        BigDecimal result = orderService.getTotalSales();

        // Assert
        assertEquals(expectedTotal, result);
        verify(orderRepository).getTotalSales();
    }

    @Test
    @DisplayName("Should return zero when no sales exist")
    void getTotalSales_NoSales() {
        // Arrange
        when(orderRepository.getTotalSales()).thenReturn(null);

        // Act
        BigDecimal result = orderService.getTotalSales();

        // Assert
        assertEquals(BigDecimal.ZERO, result);
        verify(orderRepository).getTotalSales();
    }
}