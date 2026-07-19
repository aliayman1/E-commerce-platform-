package com.microservices.pro.order.events;

public record PaymentFailedEvent(String orderId, String reason) {}