package com.microservices.pro.inventory.events;

public record InventoryReservedEvent(String orderId, String productId, int quantity) {}