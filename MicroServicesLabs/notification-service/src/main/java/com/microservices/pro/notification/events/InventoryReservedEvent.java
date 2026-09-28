package com.microservices.pro.notification.events;

public record InventoryReservedEvent(String orderId, String productId, int quantity) {}