package com.microservices.pro.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductCommandService commandService;

    @MockBean
    ProductQueryService queryService;

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 200 with the flattened summary")
    void getProduct_found_returns200() throws Exception {
        // Given
        var summary = new ProductSummaryProjection(1L, "Laptop", new BigDecimal("999.99"), "Electronics");
        when(queryService.findById(1L)).thenReturn(Optional.of(summary));

        // When + Then
        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.categoryName").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/v1/products/{id} returns 404 when not found")
    void getProduct_notFound_returns404() throws Exception {
        when(queryService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/products returns 201 with the created product")
    void createProduct_returns201() throws Exception {
        var request = new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics");
        var created = new Product(1L, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics");
        when(commandService.create(any(Product.class))).thenReturn(created);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    @DisplayName("POST /api/v1/products with missing name returns 400")
    void createProduct_missingName_returns400() throws Exception {
        var badRequest = new Product(null, null, "Gaming laptop", new BigDecimal("999.99"), "Electronics");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/products with non-positive price returns 400")
    void createProduct_invalidPrice_returns400() throws Exception {
        var badRequest = new Product(null, "Bad", null, new BigDecimal("-1"), "ELECTRONICS");
        when(commandService.create(any(Product.class)))
                .thenThrow(new InvalidProductException("Price must be positive"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isBadRequest());
    }
}
