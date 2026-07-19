package com.microservices.pro.payment.events;

public record InventoryReservedEvent(String orderId, String productId, int quantity) {}