package com.microservices.pro.order.saga;

import java.math.BigDecimal;

public record ProcessPaymentCommand(String orderId, BigDecimal amount) {}