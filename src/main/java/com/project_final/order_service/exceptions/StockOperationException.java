package com.project_final.order_service.exceptions;

public class StockOperationException extends RuntimeException {
    private final Long productId;
    private final String operation;
    private final Integer quantity;

    public StockOperationException(Long productId, String operation, Integer quantity, String message) {
        super(String.format("Error en operación de stock '%s' para producto %d con cantidad %d: %s",
                operation, productId, quantity, message));
        this.productId = productId;
        this.operation = operation;
        this.quantity = quantity;
    }

    public StockOperationException(String message) {
        super(message);
        this.productId = null;
        this.operation = null;
        this.quantity = null;
    }

    public Long getProductId() {
        return productId;
    }

    public String getOperation() {
        return operation;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
