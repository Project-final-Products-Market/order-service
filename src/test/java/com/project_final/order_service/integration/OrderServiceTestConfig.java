package com.project_final.order_service.integration;

import com.project_final.order_service.Dto.ProductDto;
import com.project_final.order_service.Dto.UserDto;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;
import org.mockito.Mockito;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@TestConfiguration
public class OrderServiceTestConfig {

    @Bean
    @Primary
    public RestTemplate mockRestTemplate() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);

        // Mock User Service - Usuario válido
        UserDto validUser = new UserDto(1L, "Test User", "test@example.com");
        when(mockRestTemplate.getForObject(eq("http://localhost:8081/api/users/1"), eq(UserDto.class)))
                .thenReturn(validUser);

        // Mock User Service - Usuario no encontrado
        when(mockRestTemplate.getForObject(eq("http://localhost:8081/api/users/999"), eq(UserDto.class)))
                .thenReturn(null);

        // Mock Product Service - Producto válido
        ProductDto validProduct = new ProductDto(1L, "Test Product", "Test Description",
                BigDecimal.valueOf(50.0), 100);
        when(mockRestTemplate.getForObject(eq("http://localhost:8082/api/products/1"), eq(ProductDto.class)))
                .thenReturn(validProduct);

        // Mock Product Service - Producto no encontrado
        when(mockRestTemplate.getForObject(eq("http://localhost:8082/api/products/999"), eq(ProductDto.class)))
                .thenReturn(null);

        // Mock Stock Check - Stock suficiente
        when(mockRestTemplate.getForObject(contains("/check-stock"), eq(Boolean.class)))
                .thenReturn(true);

        // Mock Stock Operations - Operaciones exitosas
        when(mockRestTemplate.getForObject(contains("/reduce-stock"), eq(Boolean.class)))
                .thenReturn(true);
        when(mockRestTemplate.getForObject(contains("/increase-stock"), eq(Boolean.class)))
                .thenReturn(true);

        return mockRestTemplate;
    }
}
