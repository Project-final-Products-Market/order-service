package com.project_final.order_service.exceptions;

import com.project_final.order_service.model.Order;

public class OrderCancellationException extends RuntimeException {
    private final Long orderId;
    private final Order.OrderStatus currentStatus;

    public OrderCancellationException(Long orderId, Order.OrderStatus currentStatus) {
        super(String.format("No se puede cancelar la orden %d en estado %s", orderId, currentStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
    }

    public OrderCancellationException(String message) {
        super(message);
        this.orderId = null;
        this.currentStatus = null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Order.OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
