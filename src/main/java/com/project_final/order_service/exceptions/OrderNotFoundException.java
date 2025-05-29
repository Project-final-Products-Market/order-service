package com.project_final.order_service.exceptions;

public class OrderNotFoundException extends RuntimeException {
    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Orden no encontrada con ID: " + orderId);
        this.orderId = orderId;
    }

    public OrderNotFoundException(String message) {
        super(message);
        this.orderId = null;
    }

    public Long getOrderId() {
        return orderId;
    }
}
