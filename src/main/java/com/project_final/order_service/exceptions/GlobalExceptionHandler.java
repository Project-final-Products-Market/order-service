
package com.project_final.order_service.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // EXCEPCIONES ESPECÍFICAS DE ÓRDENES

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFoundException(
            OrderNotFoundException ex, WebRequest request) {

        logger.warn("Orden no encontrada: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ORDER_NOT_FOUND")
                .message(ex.getMessage())
                .details("La orden solicitada no existe en el sistema")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.NOT_FOUND.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(OrderValidationException.class)
    public ResponseEntity<ErrorResponse> handleOrderValidationException(
            OrderValidationException ex, WebRequest request) {

        logger.warn("Error de validación de orden: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ORDER_VALIDATION_ERROR")
                .message(ex.getMessage())
                .details("Los datos de la orden no son válidos")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .field(ex.getField())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStockException(
            InsufficientStockException ex, WebRequest request) {

        logger.warn("Stock insuficiente: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("productId", ex.getProductId());
        additionalInfo.put("requestedQuantity", ex.getRequestedQuantity());
        additionalInfo.put("availableStock", ex.getAvailableStock());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INSUFFICIENT_STOCK")
                .message(ex.getMessage())
                .details("No hay suficiente stock disponible para completar la orden")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.CONFLICT.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(OrderStatusException.class)
    public ResponseEntity<ErrorResponse> handleOrderStatusException(
            OrderStatusException ex, WebRequest request) {

        logger.warn("Error de estado de orden: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("orderId", ex.getOrderId());
        additionalInfo.put("currentStatus", ex.getCurrentStatus());
        additionalInfo.put("requestedStatus", ex.getRequestedStatus());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ORDER_STATUS_ERROR")
                .message(ex.getMessage())
                .details("Transición de estado no permitida")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderCancellationException.class)
    public ResponseEntity<ErrorResponse> handleOrderCancellationException(
            OrderCancellationException ex, WebRequest request) {

        logger.warn("Error al cancelar orden: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("orderId", ex.getOrderId());
        additionalInfo.put("currentStatus", ex.getCurrentStatus());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("ORDER_CANCELLATION_ERROR")
                .message(ex.getMessage())
                .details("No se puede cancelar la orden en su estado actual")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StockOperationException.class)
    public ResponseEntity<ErrorResponse> handleStockOperationException(
            StockOperationException ex, WebRequest request) {

        logger.error("Error en operación de stock: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("productId", ex.getProductId());
        additionalInfo.put("operation", ex.getOperation());
        additionalInfo.put("quantity", ex.getQuantity());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("STOCK_OPERATION_ERROR")
                .message(ex.getMessage())
                .details("Error interno en operaciones de stock")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler({UserServiceException.class, ProductServiceException.class, ExternalServiceException.class})
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            RuntimeException ex, WebRequest request) {

        logger.error("Error de servicio externo: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        if (ex instanceof UserServiceException) {
            UserServiceException userEx = (UserServiceException) ex;
            additionalInfo.put("userId", userEx.getUserId());
            additionalInfo.put("operation", userEx.getServiceOperation());
        } else if (ex instanceof ProductServiceException) {
            ProductServiceException prodEx = (ProductServiceException) ex;
            additionalInfo.put("productId", prodEx.getProductId());
            additionalInfo.put("operation", prodEx.getServiceOperation());
        } else if (ex instanceof ExternalServiceException) {
            ExternalServiceException extEx = (ExternalServiceException) ex;
            additionalInfo.put("serviceName", extEx.getServiceName());
            additionalInfo.put("operation", extEx.getOperation());
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("EXTERNAL_SERVICE_ERROR")
                .message(ex.getMessage())
                .details("Error de comunicación con servicios externos")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // EXCEPCIONES GENERALES DE SPRING

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {

        logger.warn("Error de validación: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        StringBuilder message = new StringBuilder("Errores de validación: ");

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
            message.append(error.getField())
                    .append(" - ")
                    .append(error.getDefaultMessage())
                    .append("; ");
        }

        // Crear additionalInfo con los errores de campo
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("fieldErrors", fieldErrors);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("VALIDATION_ERROR")
                .message(message.toString())
                .details("Los datos proporcionados no son válidos")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex, WebRequest request) {

        logger.error("Error de formato de datos: {}", ex.getMessage());

        String message = "Formato de datos inválido";
        String details = "Los datos enviados no pueden ser procesados. Verifique el formato JSON.";

        // Detectar tipos específicos de errores de JSON
        if (ex.getMessage().contains("JSON parse error")) {
            message = "Error en el formato JSON";
            details = "El JSON enviado tiene errores de sintaxis";
        } else if (ex.getMessage().contains("Required request body is missing")) {
            message = "Cuerpo de petición faltante";
            details = "La petición debe incluir un cuerpo JSON válido";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("BAD_REQUEST")
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex, WebRequest request) {

        logger.error("Parámetro faltante: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("missingParameter", ex.getParameterName());
        additionalInfo.put("parameterType", ex.getParameterType());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("MISSING_PARAMETER")
                .message("Parámetro requerido faltante: " + ex.getParameterName())
                .details("Faltan parámetros obligatorios en la petición")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        logger.error("Tipo de dato inválido: {}", ex.getMessage());

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("parameter", ex.getName());
        additionalInfo.put("value", ex.getValue());
        additionalInfo.put("requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INVALID_PARAMETER_TYPE")
                .message("Tipo de dato inválido para el parámetro: " + ex.getName())
                .details("El formato de los datos no es correcto")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.BAD_REQUEST.value())
                .additionalInfo(additionalInfo)
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // EXCEPCIÓN GENÉRICA
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {

        logger.error("Error inesperado: ", ex); // Log completo con stack trace

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode("INTERNAL_SERVER_ERROR")
                .message("Error interno del servidor")
                .details("Ha ocurrido un error inesperado. Contacte al administrador del sistema.")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // CLASE PARA RESPUESTA DE ERROR
    public static class ErrorResponse {
        private String errorCode;
        private String message;
        private String details;
        private LocalDateTime timestamp;
        private String path;
        private int status;
        private String field;
        private Map<String, Object> additionalInfo;

        // Constructor privado para el builder
        private ErrorResponse(Builder builder) {
            this.errorCode = builder.errorCode;
            this.message = builder.message;
            this.details = builder.details;
            this.timestamp = builder.timestamp;
            this.path = builder.path;
            this.status = builder.status;
            this.field = builder.field;
            this.additionalInfo = builder.additionalInfo;
        }

        // Builder pattern
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String errorCode;
            private String message;
            private String details;
            private LocalDateTime timestamp;
            private String path;
            private int status;
            private String field;
            private Map<String, Object> additionalInfo;

            public Builder errorCode(String errorCode) {
                this.errorCode = errorCode;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder details(String details) {
                this.details = details;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public Builder status(int status) {
                this.status = status;
                return this;
            }

            public Builder field(String field) {
                this.field = field;
                return this;
            }

            public Builder additionalInfo(Map<String, Object> additionalInfo) {
                this.additionalInfo = additionalInfo;
                return this;
            }

            public ErrorResponse build() {
                return new ErrorResponse(this);
            }
        }

        // Getters
        public String getErrorCode() { return errorCode; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getPath() { return path; }
        public int getStatus() { return status; }
        public String getField() { return field; }
        public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    }
}