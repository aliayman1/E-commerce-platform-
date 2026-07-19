package com.microservices.pro.order.events;

public record InventoryReleasedEvent(String orderId) {}