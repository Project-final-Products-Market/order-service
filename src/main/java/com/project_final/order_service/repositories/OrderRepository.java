package com.project_final.order_service.repositories;

import com.project_final.order_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Buscar órdenes por usuario
    List<Order> findByUserIdOrderByOrderDateDesc(Long userId);

    // Buscar órdenes por producto
    List<Order> findByProductIdOrderByOrderDateDesc(Long productId);

    // Buscar órdenes por estado
    List<Order> findByStatusOrderByOrderDateDesc(Order.OrderStatus status);

    // Buscar órdenes por usuario y estado
    List<Order> findByUserIdAndStatusOrderByOrderDateDesc(Long userId, Order.OrderStatus status);

    // Buscar órdenes por rango de fechas
    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :startDate AND :endDate ORDER BY o.orderDate DESC")
    List<Order> findByOrderDateBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Contar órdenes por estado
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    Long countByStatus(@Param("status") Order.OrderStatus status);

    // Contar órdenes por usuario
    @Query("SELECT COUNT(o) FROM Order o WHERE o.userId = :userId")
    Long countByUserId(@Param("userId") Long userId);

    // Obtener órdenes recientes (últimas 24 horas)
    @Query("SELECT o FROM Order o WHERE o.orderDate >= :date ORDER BY o.orderDate DESC")
    List<Order> findRecentOrders(@Param("date") LocalDateTime date);

    // Calcular total de ventas
    @Query("SELECT SUM(o.totalPrice) FROM Order o WHERE o.status = 'CONFIRMED'")
    java.math.BigDecimal getTotalSales();

    // Obtener productos más vendidos
    @Query("SELECT o.productId, SUM(o.quantity) as totalQuantity FROM Order o WHERE o.status = 'CONFIRMED' GROUP BY o.productId ORDER BY totalQuantity DESC")
    List<Object[]> getMostSoldProducts();
}
