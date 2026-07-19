package com.microservices.pro.payment.events;

public record InventoryReservationFailedEvent(String orderId, String reason) {}