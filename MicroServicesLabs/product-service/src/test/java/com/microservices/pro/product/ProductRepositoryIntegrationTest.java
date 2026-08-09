package com.microservices.pro.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ProductRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ProductRepository productRepository;

    @Test
    void save_andFindById_roundTrip() {
        // Given
        var p = new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics");

        // When
        var saved = productRepository.save(p);
        var found = productRepository.findById(saved.getId());

        // Then
        assertTrue(found.isPresent());
        assertEquals("Laptop", found.get().getName());
        assertEquals(0, new BigDecimal("999.99").compareTo(found.get().getPrice()));
    }

    @Test
    void findByPriceLessThan_returnsMatchingProducts() {
        productRepository.saveAll(List.of(
                new Product(null, "Mouse", "Wireless mouse", new BigDecimal("29.99"), "Accessories"),
                new Product(null, "Monitor", "4K monitor", new BigDecimal("399.99"), "Electronics")));

        var cheap = productRepository.findByPriceLessThan(new BigDecimal("50"));

        assertEquals(1, cheap.size());
        assertEquals("Mouse", cheap.get(0).getName());
    }
}