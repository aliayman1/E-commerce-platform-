package com.microservices.pro.notification.events;

public record PaymentCompletedEvent(String orderId, String transactionId) {}