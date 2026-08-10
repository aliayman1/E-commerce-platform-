package com.microservices.pro.order.saga;

public record InventoryResultEvent(String orderId, boolean success) {}