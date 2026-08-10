package com.microservices.pro.order.saga;

public record PaymentResultEvent(String orderId, boolean success) {}