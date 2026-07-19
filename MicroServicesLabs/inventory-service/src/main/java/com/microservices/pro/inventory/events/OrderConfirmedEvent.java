package com.microservices.pro.inventory.events;

public record OrderConfirmedEvent(String orderId, String transactionId) {}