package com.microservices.pro.order.events;

public record OrderCancelledEvent(String orderId, String reason) {}