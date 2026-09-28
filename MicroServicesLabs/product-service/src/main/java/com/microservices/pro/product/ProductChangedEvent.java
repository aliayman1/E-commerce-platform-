package com.microservices.pro.product;

public record ProductChangedEvent(Long productId, String changeType) {
}
