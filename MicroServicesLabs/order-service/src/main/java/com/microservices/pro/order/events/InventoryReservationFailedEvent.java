package com.microservices.pro.order.events;

public record InventoryReservationFailedEvent(String orderId, String reason) {}