package com.microservices.pro.inventory.events;

public record OrderCancelledEvent(String orderId, String reason) {}