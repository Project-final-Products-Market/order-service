package com.project_final.order_service.exceptions;

public class UserServiceException extends RuntimeException {
    private final Long userId;
    private final String serviceOperation;

    public UserServiceException(Long userId, String serviceOperation, String message) {
        super(String.format("Error en User Service para usuario %d durante operación '%s': %s",
                userId, serviceOperation, message));
        this.userId = userId;
        this.serviceOperation = serviceOperation;
    }

    public UserServiceException(String message) {
        super(message);
        this.userId = null;
        this.serviceOperation = null;
    }

    public Long getUserId() {
        return userId;
    }

    public String getServiceOperation() {
        return serviceOperation;
    }
}