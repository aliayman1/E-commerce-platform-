package com.microservices.pro.order;

import java.math.BigDecimal;

public record OrderRequest(BigDecimal amount) {}