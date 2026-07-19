package com.microservices.pro.payment.events;

public record PaymentCompletedEvent(String orderId, String transactionId) {}