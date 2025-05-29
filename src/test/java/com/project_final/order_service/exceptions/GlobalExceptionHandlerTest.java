package com.project_final.order_service.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project_final.order_service.model.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // TESTS DE EXCEPCIONES ESPECÍFICAS DE ÓRDENES

    @Test
    void shouldHandleOrderNotFoundException() throws Exception {
        MvcResult result = mockMvc.perform(get("/test/order-not-found/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Orden no encontrada con ID: 999"))
                .andExpect(jsonPath("$.details").value("La orden solicitada no existe en el sistema"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").exists())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertTrue(content.contains("ORDER_NOT_FOUND"));
    }

    @Test
    void shouldHandleOrderValidationException() throws Exception {
        mockMvc.perform(get("/test/order-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("ORDER_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Error de validación en campo 'quantity' con valor '-1': La cantidad debe ser positiva"))
                .andExpect(jsonPath("$.details").value("Los datos de la orden no son válidos"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.field").value("quantity"));
    }

    @Test
    void shouldHandleInsufficientStockException() throws Exception {
        mockMvc.perform(get("/test/insufficient-stock"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.message").value("Stock insuficiente para el producto ID 1. Solicitado: 100, Disponible: 5"))
                .andExpect(jsonPath("$.details").value("No hay suficiente stock disponible para completar la orden"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.additionalInfo.productId").value(1))
                .andExpect(jsonPath("$.additionalInfo.requestedQuantity").value(100))
                .andExpect(jsonPath("$.additionalInfo.availableStock").value(5));
    }

    @Test
    void shouldHandleOrderStatusException() throws Exception {
        mockMvc.perform(get("/test/order-status"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("ORDER_STATUS_ERROR"))
                .andExpect(jsonPath("$.message").value("No se puede cambiar el estado de la orden 1 de DELIVERED a PENDING"))
                .andExpect(jsonPath("$.details").value("Transición de estado no permitida"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.additionalInfo.orderId").value(1))
                .andExpect(jsonPath("$.additionalInfo.currentStatus").value("DELIVERED"))
                .andExpect(jsonPath("$.additionalInfo.requestedStatus").value("PENDING"));
    }

    @Test
    void shouldHandleOrderCancellationException() throws Exception {
        mockMvc.perform(get("/test/order-cancellation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("ORDER_CANCELLATION_ERROR"))
                .andExpect(jsonPath("$.message").value("No se puede cancelar la orden 1 en estado DELIVERED"))
                .andExpect(jsonPath("$.details").value("No se puede cancelar la orden en su estado actual"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleStockOperationException() throws Exception {
        mockMvc.perform(get("/test/stock-operation"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("STOCK_OPERATION_ERROR"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.additionalInfo.productId").value(1))
                .andExpect(jsonPath("$.additionalInfo.operation").value("reduce"))
                .andExpect(jsonPath("$.additionalInfo.quantity").value(10));
    }

    @Test
    void shouldHandleUserServiceException() throws Exception {
        mockMvc.perform(get("/test/user-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("EXTERNAL_SERVICE_ERROR"))
                .andExpect(jsonPath("$.message").value("Error en User Service para usuario 1 durante operación 'getUserById': Usuario no disponible"))
                .andExpect(jsonPath("$.details").value("Error de comunicación con servicios externos"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void shouldHandleProductServiceException() throws Exception {
        mockMvc.perform(get("/test/product-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("EXTERNAL_SERVICE_ERROR"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void shouldHandleExternalServiceException() throws Exception {
        mockMvc.perform(get("/test/external-service"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("EXTERNAL_SERVICE_ERROR"))
                .andExpect(jsonPath("$.status").value(503));
    }

    //  TESTS DE EXCEPCIONES GENERALES DE SPRING

    @Test
    void shouldHandleHttpMessageNotReadableException() throws Exception {
        String invalidJson = "{ invalid json }";

        mockMvc.perform(post("/test/create-order")
                        .content("{ invalid json }")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Error en el formato JSON"))
                .andExpect(jsonPath("$.details").value("El JSON enviado tiene errores de sintaxis"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleMissingServletRequestParameterException() throws Exception {
        mockMvc.perform(get("/test/missing-param"))  // No se pasa el parámetro 'status'
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Parámetro requerido faltante: status"))
                .andExpect(jsonPath("$.details").value("Faltan parámetros obligatorios en la petición"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/type-mismatch/invalid-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_TYPE"))
                .andExpect(jsonPath("$.message").value("Tipo de dato inválido para el parámetro: id"))
                .andExpect(jsonPath("$.details").value("El formato de los datos no es correcto"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.details").value("Ha ocurrido un error inesperado. Contacte al administrador del sistema."))
                .andExpect(jsonPath("$.status").value(500));
    }

    //  CONTROLADOR DE PRUEBA

    @RestController
    static class TestController {

        @GetMapping("/test/order-not-found/{id}")
        public void orderNotFound(@PathVariable Long id) {
            throw new OrderNotFoundException(id);
        }

        @GetMapping("/test/order-validation")
        public void orderValidation() {
            throw new OrderValidationException("quantity", -1, "La cantidad debe ser positiva");
        }

        @GetMapping("/test/insufficient-stock")
        public void insufficientStock() {
            throw new InsufficientStockException(1L, 100, 5);
        }

        @GetMapping("/test/order-status")
        public void orderStatus() {
            throw new OrderStatusException(1L, Order.OrderStatus.DELIVERED, Order.OrderStatus.PENDING);
        }

        @GetMapping("/test/order-cancellation")
        public void orderCancellation() {
            throw new OrderCancellationException(1L, Order.OrderStatus.DELIVERED);
        }

        @GetMapping("/test/stock-operation")
        public void stockOperation() {
            throw new StockOperationException(1L, "reduce", 10, "Error en operación");
        }

        @GetMapping("/test/user-service")
        public void userService() {
            throw new UserServiceException(1L, "getUserById", "Usuario no disponible");
        }

        @GetMapping("/test/product-service")
        public void productService() {
            throw new ProductServiceException(1L, "getProductById", "Producto no disponible");
        }

        @GetMapping("/test/external-service")
        public void externalService() {
            throw new ExternalServiceException("external-api", "getData", "Servicio no disponible");
        }

        @PostMapping("/test/create-order")
        public void createOrder(@RequestBody Object request) {
            // Este método causará HttpMessageNotReadableException si el JSON es inválido
        }

        @GetMapping("/test/missing-param")
        public void missingParam(@RequestParam String status) {
            // Este método causará MissingServletRequestParameterException
        }

        @GetMapping("/test/type-mismatch/{id}")
        public void typeMismatch(@PathVariable Long id) {
            // Este método causará MethodArgumentTypeMismatchException
        }

        @GetMapping("/test/generic-error")
        public void genericError() {
            throw new RuntimeException("Error genérico de prueba");
        }
    }
}
