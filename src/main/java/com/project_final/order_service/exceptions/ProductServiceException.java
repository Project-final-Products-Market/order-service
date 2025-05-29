package com.project_final.order_service.exceptions;

public class ProductServiceException extends RuntimeException {
    private final Long productId;
    private final String serviceOperation;

    public ProductServiceException(Long productId, String serviceOperation, String message) {
        super(String.format("Error en Product Service para producto %d durante operación '%s': %s",
                productId, serviceOperation, message));
        this.productId = productId;
        this.serviceOperation = serviceOperation;
    }

    public ProductServiceException(String message) {
        super(message);
        this.productId = null;
        this.serviceOperation = null;
    }

    public Long getProductId() {
        return productId;
    }

    public String getServiceOperation() {
        return serviceOperation;
    }
}
