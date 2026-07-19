package com.microservices.pro.payment.events;

public record PaymentFailedEvent(String orderId, String reason) {}