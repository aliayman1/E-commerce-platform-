package com.microservices.pro.order.events;

public record OrderConfirmedEvent(String orderId, String transactionId) {}