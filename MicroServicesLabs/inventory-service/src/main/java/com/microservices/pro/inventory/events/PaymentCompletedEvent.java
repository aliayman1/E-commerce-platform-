package com.microservices.pro.inventory.events;

public record PaymentCompletedEvent(String orderId, String transactionId) {}