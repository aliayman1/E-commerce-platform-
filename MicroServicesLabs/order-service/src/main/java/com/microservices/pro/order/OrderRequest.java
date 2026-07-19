package com.microservices.pro.order;

import java.math.BigDecimal;

public record OrderRequest(String productId, int quantity, BigDecimal amount, String customerId) {}