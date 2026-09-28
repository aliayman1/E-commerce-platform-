package com.microservices.pro.product;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductQueryService queryService;

    @ParameterizedTest
    @CsvSource({
            "SILVER,   5",
            "GOLD,    10",
            "PLATINUM,15"
    })
    void calcDiscount(String tier, int expected) {
        assertEquals(expected, queryService.calcDiscount(tier));
    }
}
