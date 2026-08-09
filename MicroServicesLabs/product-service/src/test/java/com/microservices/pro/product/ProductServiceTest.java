package com.microservices.pro.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @ParameterizedTest
    @CsvSource({
            "SILVER,   5",
            "GOLD,    10",
            "PLATINUM,15"
    })
    void calcDiscount(String tier, int expected) {
        assertEquals(expected, productService.calcDiscount(tier));
    }

    @Test
    void createProduct_savesProductWithCorrectFields() {
        // Given
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);

        // When
        productService.save(new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        // Then: verify the EXACT object saved to repository
        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("Laptop", saved.getName());
        assertEquals("Gaming laptop", saved.getDescription());
        assertEquals(new BigDecimal("999.99"), saved.getPrice());
        assertEquals("Electronics", saved.getCategory());
    }
}