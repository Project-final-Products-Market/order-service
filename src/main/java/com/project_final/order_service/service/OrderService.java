package com.project_final.order_service.service;

import com.project_final.order_service.model.Order;
import com.project_final.order_service.Dto.CreateOrderRequest;
import com.project_final.order_service.Dto.ProductDto;
import com.project_final.order_service.Dto.UserDto;
import com.project_final.order_service.repositories.OrderRepository;
import com.project_final.order_service.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    // URLs de otros servicios
    private final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products";
    private final String USER_SERVICE_URL = "http://localhost:8081/api/users";

    // Crear orden
    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        logger.info("Iniciando creación de orden para usuario {} y producto {}",
                request.getUserId(), request.getProductId());

        // Validar entrada
        validateCreateOrderRequest(request);

        try {
            // Validar usuario
            UserDto user = getUserById(request.getUserId());
            if (user == null) {
                throw new UserServiceException(request.getUserId(), "getUserById",
                        "Usuario no encontrado");
            }

            // Validar producto y stock
            ProductDto product = getProductById(request.getProductId());
            if (product == null) {
                throw new ProductServiceException(request.getProductId(), "getProductById",
                        "Producto no encontrado");
            }

            // Verificar stock disponible
            if (!checkProductStock(request.getProductId(), request.getQuantity())) {
                throw new InsufficientStockException(request.getProductId(),
                        request.getQuantity(), product.getStock());
            }

            // Calcular precio total
            BigDecimal totalPrice = product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

            // Crear la orden
            Order order = new Order(request.getUserId(), request.getProductId(),
                    request.getQuantity(), totalPrice);

            // Reducir stock del producto (comentado temporalmente)
            // if (!reduceProductStock(request.getProductId(), request.getQuantity())) {
            //     throw new StockOperationException(request.getProductId(), "reduce",
            //         request.getQuantity(), "No se pudo reducir el stock");
            // }

            // Guardar orden
            order.setStatus(Order.OrderStatus.CONFIRMED);
            Order savedOrder = orderRepository.save(order);

            logger.info("Orden creada exitosamente con ID: {}", savedOrder.getId());
            return savedOrder;

        } catch (UserServiceException | ProductServiceException | InsufficientStockException |
                 StockOperationException e) {
            logger.error("Error específico creando orden: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Error inesperado creando orden: {}", e.getMessage(), e);
            throw new ExternalServiceException("order-service", "createOrder",
                    "Error inesperado durante la creación de la orden");
        }
    }

    // Obtener todas las órdenes
    public List<Order> getAllOrders() {
        logger.debug("Obteniendo todas las órdenes");
        return orderRepository.findAll();
    }

    // Obtener orden por ID
    public Optional<Order> getOrderById(Long id) {
        logger.debug("Buscando orden con ID: {}", id);

        if (id == null || id <= 0) {
            throw new OrderValidationException("id", id, "El ID de la orden debe ser un número positivo");
        }

        return orderRepository.findById(id);
    }

    // Obtener orden por ID (método que lanza excepción si no existe)
    public Order getOrderByIdOrThrow(Long id) {
        return getOrderById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    // Obtener órdenes por usuario
    public List<Order> getOrdersByUserId(Long userId) {
        logger.debug("Obteniendo órdenes para usuario: {}", userId);

        if (userId == null || userId <= 0) {
            throw new OrderValidationException("userId", userId,
                    "El ID del usuario debe ser un número positivo");
        }

        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    // Obtener órdenes por producto
    public List<Order> getOrdersByProductId(Long productId) {
        logger.debug("Obteniendo órdenes para producto: {}", productId);

        if (productId == null || productId <= 0) {
            throw new OrderValidationException("productId", productId,
                    "El ID del producto debe ser un número positivo");
        }

        return orderRepository.findByProductIdOrderByOrderDateDesc(productId);
    }

    // Obtener órdenes por estado
    public List<Order> getOrdersByStatus(Order.OrderStatus status) {
        logger.debug("Obteniendo órdenes con estado: {}", status);

        if (status == null) {
            throw new OrderValidationException("status", null, "El estado no puede ser nulo");
        }

        return orderRepository.findByStatusOrderByOrderDateDesc(status);
    }

    // Actualizar estado de orden
    @Transactional
    public Order updateOrderStatus(Long id, Order.OrderStatus newStatus) {
        logger.info("Actualizando estado de orden {} a {}", id, newStatus);

        if (newStatus == null) {
            throw new OrderValidationException("status", null, "El nuevo estado no puede ser nulo");
        }

        Order order = getOrderByIdOrThrow(id);
        Order.OrderStatus oldStatus = order.getStatus();

        // Validar transición de estado
        validateStatusTransition(order, oldStatus, newStatus);

        // Actualizar el estado primero
        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);
        logger.info("Estado de orden {} actualizado de {} a {}", id, oldStatus, newStatus);

        // Si se cancela una orden confirmada, intentar devolver stock (sin fallar la transacción)
        if (oldStatus == Order.OrderStatus.CONFIRMED && newStatus == Order.OrderStatus.CANCELLED) {
            logger.info("Devolviendo stock para orden cancelada: {}", id);
            try {
                if (!increaseProductStock(order.getProductId(), order.getQuantity())) {
                    logger.warn("No se pudo devolver el stock para la orden {}, pero la cancelación se mantuvo", id);
                }
            } catch (StockOperationException e) {
                logger.warn("Error devolviendo stock para orden cancelada {}: {}", id, e.getMessage());
                // No propagamos la excepción - la orden ya está cancelada
            } catch (Exception e) {
                logger.warn("Error inesperado devolviendo stock para orden {}: {}", id, e.getMessage());
                // No propagamos la excepción - la orden ya está cancelada
            }
        }

        return updatedOrder;
    }

    // Cancelar orden
    @Transactional
    public Order cancelOrder(Long id) {
        logger.info("Cancelando orden: {}", id);

        Order order = getOrderByIdOrThrow(id);

        // Verificar si se puede cancelar
        if (!canCancelOrder(order)) {
            throw new OrderCancellationException(id, order.getStatus());
        }

        return updateOrderStatus(id, Order.OrderStatus.CANCELLED);
    }

    // Eliminar orden
    @Transactional
    public void deleteOrder(Long id) {
        logger.info("Iniciando eliminación de orden: {}", id);

        // Validar ID
        if (id == null || id <= 0) {
            throw new OrderValidationException("id", id, "El ID de la orden debe ser un número positivo");
        }

        // Verificar que la orden existe
        Order order = getOrderByIdOrThrow(id);

        // Validar que se puede eliminar la orden
        if (!canDeleteOrder(order)) {
            throw new OrderDeletionException(id, order.getStatus(),
                    "No se puede eliminar una orden con estado " + order.getStatus());
        }

        try {
            // Si la orden estaba confirmada, intentar devolver el stock
            if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
                logger.info("Devolviendo stock antes de eliminar orden confirmada: {}", id);
                try {
                    if (!increaseProductStock(order.getProductId(), order.getQuantity())) {
                        logger.warn("No se pudo devolver el stock para la orden {}, pero se procederá con la eliminación", id);
                    }
                } catch (Exception e) {
                    logger.warn("Error devolviendo stock para orden {}: {}. Se procederá con la eliminación", id, e.getMessage());
                }
            }

            // Eliminar la orden
            orderRepository.deleteById(id);
            logger.info("Orden {} eliminada exitosamente", id);

        } catch (Exception e) {
            logger.error("Error eliminando orden {}: {}", id, e.getMessage(), e);
            throw new ExternalServiceException("order-service", "deleteOrder",
                    "Error inesperado durante la eliminación de la orden");
        }
    }

    // Obtener órdenes recientes
    public List<Order> getRecentOrders() {
        logger.debug("Obteniendo órdenes recientes (últimas 24 horas)");
        LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
        return orderRepository.findRecentOrders(yesterday);
    }

    // Obtener estadísticas
    public Long getTotalOrders() {
        logger.debug("Obteniendo total de órdenes");
        return orderRepository.count();
    }

    public Long getOrdersByStatus(String status) {
        logger.debug("Obteniendo cantidad de órdenes con estado: {}", status);

        if (status == null || status.trim().isEmpty()) {
            throw new OrderValidationException("status", status, "El estado no puede estar vacío");
        }

        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            return orderRepository.countByStatus(orderStatus);
        } catch (IllegalArgumentException e) {
            throw new OrderValidationException("status", status,
                    "Estado inválido. Estados válidos: PENDING, CONFIRMED, CANCELLED, DELIVERED");
        }
    }

    public BigDecimal getTotalSales() {
        logger.debug("Calculando total de ventas");
        BigDecimal total = orderRepository.getTotalSales();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ========== MÉTODOS PRIVADOS PARA VALIDACIONES ==========

    private void validateCreateOrderRequest(CreateOrderRequest request) {
        if (request == null) {
            throw new OrderValidationException("Datos de orden requeridos");
        }

        if (request.getUserId() == null || request.getUserId() <= 0) {
            throw new OrderValidationException("userId", request.getUserId(),
                    "El ID del usuario debe ser un número positivo");
        }

        if (request.getProductId() == null || request.getProductId() <= 0) {
            throw new OrderValidationException("productId", request.getProductId(),
                    "El ID del producto debe ser un número positivo");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new OrderValidationException("quantity", request.getQuantity(),
                    "La cantidad debe ser un número positivo");
        }

        if (request.getQuantity() > 1000) {
            throw new OrderValidationException("quantity", request.getQuantity(),
                    "La cantidad no puede exceder 1000 unidades");
        }
    }

    private void validateStatusTransition(Order order, Order.OrderStatus from, Order.OrderStatus to) {
        // Reglas de transición de estado
        switch (from) {
            case PENDING:
                if (to != Order.OrderStatus.CONFIRMED && to != Order.OrderStatus.CANCELLED) {
                    throw new OrderStatusException(order.getId(), from, to);
                }
                break;
            case CONFIRMED:
                if (to != Order.OrderStatus.DELIVERED && to != Order.OrderStatus.CANCELLED) {
                    throw new OrderStatusException(order.getId(), from, to);
                }
                break;
            case DELIVERED:
                // Una vez entregado, no se puede cambiar el estado
                throw new OrderStatusException(order.getId(), from, to);
            case CANCELLED:
                // Una vez cancelado, no se puede cambiar el estado
                throw new OrderStatusException(order.getId(), from, to);
        }
    }

    private boolean canCancelOrder(Order order) {
        return order.getStatus() == Order.OrderStatus.PENDING ||
                order.getStatus() == Order.OrderStatus.CONFIRMED;
    }

    // Método privado para validar si se puede eliminar una orden
    private boolean canDeleteOrder(Order order) {
        // Solo se pueden eliminar órdenes que estén en estado PENDING, CONFIRMED o CANCELLED
        // No se pueden eliminar órdenes DELIVERED
        return order.getStatus() != Order.OrderStatus.DELIVERED;
    }

    // ========== MÉTODOS PRIVADOS PARA COMUNICACIÓN CON OTROS SERVICIOS ==========

    private UserDto getUserById(Long userId) {
        try {
            String url = USER_SERVICE_URL + "/" + userId;
            logger.debug("Consultando usuario en: {}", url);
            return restTemplate.getForObject(url, UserDto.class);
        } catch (RestClientException e) {
            logger.error("Error consultando User Service para usuario {}: {}", userId, e.getMessage());
            throw new UserServiceException(userId, "getUserById", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado consultando User Service: {}", e.getMessage());
            throw new ExternalServiceException("user-service", "getUserById", e.getMessage());
        }
    }

    private ProductDto getProductById(Long productId) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId;
            logger.debug("Consultando producto en: {}", url);
            return restTemplate.getForObject(url, ProductDto.class);
        } catch (RestClientException e) {
            logger.error("Error consultando Product Service para producto {}: {}", productId, e.getMessage());
            throw new ProductServiceException(productId, "getProductById", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado consultando Product Service: {}", e.getMessage());
            throw new ExternalServiceException("product-service", "getProductById", e.getMessage());
        }
    }

    private boolean checkProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/check-stock?quantity=" + quantity;
            logger.debug("Verificando stock en: {}", url);
            Boolean hasStock = restTemplate.getForObject(url, Boolean.class);
            return hasStock != null && hasStock;
        } catch (RestClientException e) {
            logger.error("Error verificando stock para producto {}: {}", productId, e.getMessage());
            throw new ProductServiceException(productId, "checkStock", e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado verificando stock: {}", e.getMessage());
            return false; // En caso de error, asumimos que no hay stock
        }
    }

    private boolean reduceProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/reduce-stock?quantity=" + quantity;
            logger.debug("Reduciendo stock en: {}", url);
            Boolean success = restTemplate.getForObject(url, Boolean.class);
            return success != null && success;
        } catch (RestClientException e) {
            logger.error("Error reduciendo stock para producto {}: {}", productId, e.getMessage());
            throw new StockOperationException(productId, "reduce", quantity, e.getMessage());
        } catch (Exception e) {
            logger.error("Error inesperado reduciendo stock: {}", e.getMessage());
            throw new StockOperationException(productId, "reduce", quantity,
                    "Error inesperado durante la operación de stock");
        }
    }

    private boolean increaseProductStock(Long productId, Integer quantity) {
        try {
            String url = PRODUCT_SERVICE_URL + "/" + productId + "/increase-stock?quantity=" + quantity;
            logger.debug("Aumentando stock en: {}", url);
            Boolean success = restTemplate.getForObject(url, Boolean.class);
            boolean result = success != null && success;

            if (!result) {
                logger.warn("Product service retornó false al intentar aumentar stock para producto {}", productId);
            }

            return result;
        } catch (RestClientException e) {
            logger.error("Error aumentando stock para producto {}: {}", productId, e.getMessage());
            // En contexto de cancelación, registramos el error pero no lanzamos excepción
            return false;
        } catch (Exception e) {
            logger.error("Error inesperado aumentando stock para producto {}: {}", productId, e.getMessage());
            return false;
        }
    }
}
