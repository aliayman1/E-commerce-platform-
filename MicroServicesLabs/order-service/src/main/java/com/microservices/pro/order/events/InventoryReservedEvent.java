package com.microservices.pro.order.events;

public record InventoryReservedEvent(String orderId, String productId, int quantity) {}