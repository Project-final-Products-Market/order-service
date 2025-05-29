package com.project_final.order_service.exceptions;

public class OrderValidationException extends RuntimeException {
    private final String field;
    private final Object value;

    public OrderValidationException(String message) {
        super(message);
        this.field = null;
        this.value = null;
    }

    public OrderValidationException(String field, Object value, String message) {
        super(String.format("Error de validación en campo '%s' con valor '%s': %s", field, value, message));
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public Object getValue() {
        return value;
    }
}