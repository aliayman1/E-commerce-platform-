package com.microservices.pro.order.events;

public record PaymentCompletedEvent(String orderId, String transactionId) {}