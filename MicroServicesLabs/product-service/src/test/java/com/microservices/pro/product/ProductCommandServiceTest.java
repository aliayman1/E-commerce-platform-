package com.microservices.pro.product;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductCommandService commandService;

    @Test
    void create_invalidPrice_rejectedWithoutSavingOrPublishing() {
        var bad = new Product(null, "Bad", null, new BigDecimal("-1"), "ELECTRONICS");

        assertThrows(InvalidProductException.class, () -> commandService.create(bad));

        verify(productRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_publishesProductChangedEvent() {
        when(productRepository.save(any(Product.class)))
                .thenReturn(new Product(42L, "Laptop", null, new BigDecimal("999.99"), "ELECTRONICS"));

        commandService.create(new Product(null, "Laptop", null, new BigDecimal("999.99"), "ELECTRONICS"));

        verify(eventPublisher).publishEvent(new ProductChangedEvent(42L, "CREATED"));
    }

    @Test
    void create_savesProductWithCorrectFields() {
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(any(Product.class)))
                .thenReturn(new Product(1L, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        commandService.create(new Product(null, "Laptop", "Gaming laptop", new BigDecimal("999.99"), "Electronics"));

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals("Laptop", saved.getName());
        assertEquals("Gaming laptop", saved.getDescription());
        assertEquals(new BigDecimal("999.99"), saved.getPrice());
        assertEquals("Electronics", saved.getCategory());
    }
}
