package com.microservices.pro.payment;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {}