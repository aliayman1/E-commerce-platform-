package com.microservices.pro.payment;

import java.math.BigDecimal;

public record PaymentResponse(String status, String transactionId, BigDecimal amount) {}
