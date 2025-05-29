package com.project_final.order_service.exceptions;

import com.project_final.order_service.model.Order;

public class OrderDeletionException extends RuntimeException {

    private final Long orderId;
    private final Order.OrderStatus currentStatus;

    public OrderDeletionException(Long orderId, Order.OrderStatus currentStatus, String message) {
        super(String.format("No se puede eliminar la orden %d con estado %s: %s",
                orderId, currentStatus, message));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public Long getOrderId() { return orderId; }
    public Order.OrderStatus getCurrentStatus() { return currentStatus; }
}
