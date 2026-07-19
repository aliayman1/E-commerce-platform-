package com.microservices.pro.payment.events;

public record OrderConfirmedEvent(String orderId, String transactionId) {}