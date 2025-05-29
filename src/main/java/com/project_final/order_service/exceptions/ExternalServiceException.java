package com.project_final.order_service.exceptions;

public class ExternalServiceException extends RuntimeException {
    private final String serviceName;
    private final String operation;

    public ExternalServiceException(String serviceName, String operation, String message) {
        super(String.format("Error en servicio externo '%s' durante operación '%s': %s",
                serviceName, operation, message));
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public ExternalServiceException(String serviceName, String operation, Throwable cause) {
        super(String.format("Error en servicio externo '%s' durante operación '%s'", serviceName, operation), cause);
        this.serviceName = serviceName;
        this.operation = operation;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperation() {
        return operation;
    }
}
