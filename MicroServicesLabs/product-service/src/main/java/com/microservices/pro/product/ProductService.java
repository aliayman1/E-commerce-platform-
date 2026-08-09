package com.microservices.pro.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private static final String CACHE_NAME = "products";

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = CACHE_NAME, key = "'all'")
    public List<Product> findAll() {
        log.info("[CACHE MISS] Loading all products from database");
        return productRepository.findAll();
    }

    @Cacheable(value = CACHE_NAME, key = "#id")
    public Optional<Product> findById(Long id) {
        log.info("[CACHE MISS] Loading product {} from database", id);
        return productRepository.findById(id);
    }

    @CacheEvict(value = CACHE_NAME, key = "'all'")
    public Product save(Product product) {
        return productRepository.save(product);
    }

    // Evicts both the single-product entry and the all-products list in one pass
    // calling a second @CacheEvict method from here would bypass the proxy (self-invocation)
    // and silently never evict, so both keys are declared on the write method itself.
    @Caching(evict = {
            @CacheEvict(value = CACHE_NAME, key = "#id"),
            @CacheEvict(value = CACHE_NAME, key = "'all'")
    })
    public Product update(Long id, Product product) {
        log.info("[CACHE EVICT] Invalidating cache for product {}", id);
        product.setId(id);
        return productRepository.save(product);
    }

    @Caching(evict = {
            @CacheEvict(value = CACHE_NAME, key = "#id"),
            @CacheEvict(value = CACHE_NAME, key = "'all'")
    })
    public void deleteById(Long id) {
        log.info("[CACHE EVICT] Invalidating cache for product {}", id);
        productRepository.deleteById(id);
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