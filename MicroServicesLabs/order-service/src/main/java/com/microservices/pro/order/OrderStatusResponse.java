package com.microservices.pro.order;

public record OrderStatusResponse(String orderId, OrderStatus status) {}