package com.microservices.pro.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ProductCacheEvictionListener {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheEvictionListener.class);
    private static final String CACHE_NAME = "products";

    private final CacheManager cacheManager;

    public ProductCacheEvictionListener(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @EventListener
    public void onProductChanged(ProductChangedEvent event) {
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evict(event.productId());
        Objects.requireNonNull(cacheManager.getCache(CACHE_NAME)).evict("all");
        log.info("[CQRS] Cache evicted for product {} due to {}", event.productId(), event.changeType());
    }
}
