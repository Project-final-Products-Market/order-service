package com.project_final.order_service.controller;

import com.project_final.order_service.exceptions.OrderValidationException;
import com.project_final.order_service.model.Order;
import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    // Crear orden
    @PostMapping
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody CreateOrderRequest request) {
        logger.info("Petición para crear orden: userId={}, productId={}, quantity={}",
                request.getUserId(), request.getProductId(), request.getQuantity());

        Order createdOrder = orderService.createOrder(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Orden creada correctamente");
        response.put("data", createdOrder);

        logger.info("Orden creada exitosamente con ID: {}", createdOrder.getId());

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Obtener todas las órdenes
    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        logger.debug("Petición para obtener todas las órdenes");
        List<Order> orders = orderService.getAllOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // Obtener orden por ID
    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        logger.debug("Petición para obtener orden con ID: {}", id);
        Optional<Order> order = orderService.getOrderById(id);
        return order.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Obtener órdenes por usuario (endpoint usado por User Service)
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getOrdersByUserId(@PathVariable Long userId) {
        logger.debug("Petición para obtener órdenes del usuario: {}", userId);
        List<Order> orders = orderService.getOrdersByUserId(userId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // Obtener órdenes por producto
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Order>> getOrdersByProductId(@PathVariable Long productId) {
        logger.debug("Petición para obtener órdenes del producto: {}", productId);
        List<Order> orders = orderService.getOrdersByProductId(productId);
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // Obtener órdenes por estado
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Order>> getOrdersByStatus(@PathVariable String status) {
        logger.debug("Petición para obtener órdenes con estado: {}", status);

        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.getOrdersByStatus(orderStatus);
            return new ResponseEntity<>(orders, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new OrderValidationException("status", status,
                    "Estado inválido. Estados válidos: PENDING, CONFIRMED, CANCELLED, DELIVERED");
        }
    }

    // Actualizar estado de orden
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        logger.info("Petición para actualizar estado de orden {} a {}", id, status);

        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            Order updatedOrder = orderService.updateOrderStatus(id, newStatus);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Estado de la orden actualizado correctamente a " + newStatus);
            response.put("data", updatedOrder);

            logger.info("Estado de orden {} actualizado correctamente a {}", id, newStatus);

            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            throw new OrderValidationException("status", status,
                    "Estado inválido. Estados válidos: PENDING, CONFIRMED, CANCELLED, DELIVERED");
        }
    }

    // Cancelar orden
    @PutMapping("/{id}/cancel")
    public ResponseEntity<Map<String, Object>> cancelOrder(@PathVariable Long id) {
        logger.info("Petición para cancelar orden: {}", id);
        Order cancelledOrder = orderService.cancelOrder(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Orden cancelada correctamente");
        response.put("data", cancelledOrder);

        logger.info("Orden {} cancelada correctamente", id);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }



    // Eliminar orden
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteOrder(@PathVariable Long id) {
        logger.info("Petición para eliminar orden: {}", id);

        orderService.deleteOrder(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Orden eliminada correctamente");

        logger.info("Orden {} eliminada correctamente", id);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // Obtener órdenes recientes
    @GetMapping("/recent")
    public ResponseEntity<List<Order>> getRecentOrders() {
        logger.debug("Petición para obtener órdenes recientes");
        List<Order> orders = orderService.getRecentOrders();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    // Obtener estadísticas
    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalOrders() {
        logger.debug("Petición para obtener total de órdenes");
        Long total = orderService.getTotalOrders();
        return new ResponseEntity<>(total, HttpStatus.OK);
    }

    @GetMapping("/stats/status/{status}")
    public ResponseEntity<Long> getOrdersByStatusCount(@PathVariable String status) {
        logger.debug("Petición para contar órdenes con estado: {}", status);
        Long count = orderService.getOrdersByStatus(status);
        return new ResponseEntity<>(count, HttpStatus.OK);
    }

    @GetMapping("/stats/sales")
    public ResponseEntity<BigDecimal> getTotalSales() {
        logger.debug("Petición para obtener total de ventas");
        BigDecimal total = orderService.getTotalSales();
        return new ResponseEntity<>(total, HttpStatus.OK);
    }
}
