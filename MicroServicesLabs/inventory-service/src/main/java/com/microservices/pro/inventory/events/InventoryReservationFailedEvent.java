package com.microservices.pro.inventory.events;

public record InventoryReservationFailedEvent(String orderId, String reason) {}