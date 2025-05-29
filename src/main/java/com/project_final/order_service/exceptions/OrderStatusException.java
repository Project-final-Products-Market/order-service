package com.project_final.order_service.exceptions;

import com.project_final.order_service.model.Order;

public class OrderStatusException extends RuntimeException {
    private final Long orderId;
    private final Order.OrderStatus currentStatus;
    private final Order.OrderStatus requestedStatus;

    public OrderStatusException(Long orderId, Order.OrderStatus currentStatus, Order.OrderStatus requestedStatus) {
        super(String.format("No se puede cambiar el estado de la orden %d de %s a %s",
                orderId, currentStatus, requestedStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public OrderStatusException(String message) {
        super(message);
        this.orderId = null;
        this.currentStatus = null;
        this.requestedStatus = null;
    }

    public Long getOrderId() {
        return orderId;
    }

    public Order.OrderStatus getCurrentStatus() {
        return currentStatus;
    }

    public Order.OrderStatus getRequestedStatus() {
        return requestedStatus;
    }
}
