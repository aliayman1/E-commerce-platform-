package com.microservices.pro.order.events;

import java.math.BigDecimal;

public record OrderPlacedEvent(
        String orderId, String productId, int quantity, BigDecimal amount, String customerId) {}