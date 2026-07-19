package com.microservices.pro.payment.events;

public record OrderCancelledEvent(String orderId, String reason) {}