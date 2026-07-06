package com.microservices.pro.order;

import java.math.BigDecimal;

public record PaymentResponse(String status, String transactionId, BigDecimal amount) {}