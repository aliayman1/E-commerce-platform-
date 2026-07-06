package com.microservices.pro.order;

import java.math.BigDecimal;

public record PaymentRequest(BigDecimal amount) {}