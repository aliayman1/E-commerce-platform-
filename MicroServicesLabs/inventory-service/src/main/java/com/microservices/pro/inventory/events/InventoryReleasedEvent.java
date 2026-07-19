package com.microservices.pro.inventory.events;

public record InventoryReleasedEvent(String orderId) {}