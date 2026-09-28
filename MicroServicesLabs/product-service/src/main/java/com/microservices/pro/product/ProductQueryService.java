package com.microservices.pro.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductQueryService {

    private static final Logger log = LoggerFactory.getLogger(ProductQueryService.class);
    private static final String CACHE_NAME = "products";

    private final ProductRepository productRepository;

    public ProductQueryService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    @Transactional(readOnly = true)
    public Optional<ProductSummaryProjection> findById(Long id) {
        log.info("[CACHE MISS] Loading product {} from database", id);
        return productRepository.findSummaryById(id);
    }

    @Cacheable(value = CACHE_NAME, key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductSummaryProjection> findAll() {
        log.info("[CACHE MISS] Loading all products from database");
        return productRepository.findAllSummaries();
    }

    public int calcDiscount(String tier) {
        return switch (tier) {
            case "SILVER" -> 5;
            case "GOLD" -> 10;
            case "PLATINUM" -> 15;
            default -> 0;
        };
    }
}
