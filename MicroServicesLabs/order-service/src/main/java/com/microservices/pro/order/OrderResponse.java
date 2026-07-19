package com.microservices.pro.order;

public record OrderResponse(String orderId, OrderStatus status, String message) {}