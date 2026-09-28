package com.microservices.pro.notification.events;

public record InventoryReservationFailedEvent(String orderId, String reason) {}