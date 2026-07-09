package com.microservices.pro.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_returnsEmptyList_whenNoProductsExist() {
        List<Product> result = productService.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void save_storesProduct_andFindById_retrievesIt() {
        Product product = new Product(null, "Laptop", "Gaming laptop", BigDecimal.valueOf(999.99), "Electronics");
        Product saved = productService.save(product);

        assertNotNull(saved.id());
        Optional<Product> found = productService.findById(saved.id());
        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().name());
    }

    @Test
    void findById_returnsEmpty_forNonExistentId() {
        Optional<Product> result = productService.findById(999L);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_removesProduct() {
        Product saved = productService.save(new Product(null, "Phone", "Smartphone", BigDecimal.valueOf(599.99), "Electronics"));

        productService.deleteById(saved.id());

        assertTrue(productService.findById(saved.id()).isEmpty());
    }

    @Test
    void findAll_returnsAllSavedProducts() {
        productService.save(new Product(null, "Laptop", "Gaming laptop", BigDecimal.valueOf(999.99), "Electronics"));
        productService.save(new Product(null, "Phone", "Smartphone", BigDecimal.valueOf(599.99), "Electronics"));

        assertEquals(2, productService.findAll().size());
    }
}