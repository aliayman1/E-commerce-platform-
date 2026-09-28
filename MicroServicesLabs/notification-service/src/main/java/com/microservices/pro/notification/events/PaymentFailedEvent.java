package com.microservices.pro.notification.events;

public record PaymentFailedEvent(String orderId, String reason) {}