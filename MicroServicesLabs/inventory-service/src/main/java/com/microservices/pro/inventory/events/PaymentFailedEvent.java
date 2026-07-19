package com.microservices.pro.inventory.events;

public record PaymentFailedEvent(String orderId, String reason) {}