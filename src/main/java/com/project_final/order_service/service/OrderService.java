package com.project_final.order_service.service;

import com.project_final.order_service.model.Order;
import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.Dto.ProductDto;
import com.project_final.order_service.Dto.UserDto;
import com.project_final.order_service.repositories.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    // URLs de otros servicios
    private final String PRODUCT_SERVICE_URL = "http://product-service/api/products";
    private final String USER_SERVICE_URL = "http://user-service/api/users";

    // Crear orden
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // Validar usuario
        UserDto user = getUserById(request.getUserId());
        if (user == null) {
            throw new RuntimeException("Usuario no encontrado con id: " + request.getUserId());
        }

        // Validar producto y stock
        ProductDto product = getProductById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("Producto no encontrado con id: " + request.getProductId());
        }

        // Verificar stock disponible
        if (!checkProductStock(request.getProductId(), request.getQuantity())) {
            throw new RuntimeException("Stock insuficiente para el producto: " + product.getName());
        }

        // Calcular precio total
        BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Crear la orden
        Order order = new Order(request.getUserId(), request.getProductId(), request.getQuantity(), totalPrice);

        // Reducir stock del producto
        if (!reduceProductStock(request.getProductId(), request.getQuantity())) {
            throw new RuntimeException("Error al reducir stock del producto");
        }

        // Guardar orden
        order.setStatus(Order.OrderStatus.CONFIRMED);
        return orderRepository.save(order);
    }

    // Obtener todas las órdenes
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // Obtener orden por ID
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    // Obtener órdenes por usuario
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // Obtener órdenes por producto
    public List<Order> getOrdersByProductId(Long productId) {
        return orderRepository.findByProductIdOrderByOrderDateDesc(productId);
    }

    // Obtener órdenes por estado
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    // Actualizar estado de orden
    @Transactional
    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Orden no encontrada con id: " + id));

        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(newStatus);

        // Si se cancela una orden confirmada, devolver stock
        if (oldStatus == Order.OrderStatus.CONFIRMED && newStatus == Order.OrderStatus.CANCELLED) {
            increaseProductStock(order.getProductId(), order.getQuantity());
        }

        return orderRepository.save(order);
    }

    // Cancelar orden
    @Transactional
    public Order cancelOrder(Long id) {
        return updateOrderStatus(id, Order.OrderStatus.CANCELLED);
    }

    // Obtener órdenes recientes
    public List<Order> getRecentOrders() {
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        return orderRepository.findRecentOrders(yesterday);
    }

    // Obtener estadísticas
    public Long getTotalOrders() {
        return orderRepository.count();
    }

    public Long getOrdersByStatus(String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.countByStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado de orden inválido: " + status);
        }
    }

    public BigDecimal getTotalSales() {
        BigDecimal total = orderRepository.getTotalSales();
        return total != null ? total : BigDecimal.ZERO;
    }

    // Métodos privados para comunicación con otros servicios
    private UserDto getUserById(Long userId) {
        try {
            String url = USER_SERVICE_URL + "/" + userId;
            return restTemplate.getForObject(url, UserDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    private ProductDto getProductById(Long productId) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId;
            return restTemplate.getForObject(url, ProductDto.class);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean checkProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/check-stock?quantity=" + quantity;
            Boolean hasStock = restTemplate.getForObject(url, Boolean.class);
            return hasStock != null && hasStock;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean reduceProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/reduce-stock?quantity=" + quantity;
            Boolean success = restTemplate.getForObject(url, Boolean.class);
            return success != null && success;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean increaseProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/increase-stock?quantity=" + quantity;
            Boolean success = restTemplate.getForObject(url, Boolean.class);
            return success != null && success;
        } catch (Exception e) {
            return false;
        }
    }
}
