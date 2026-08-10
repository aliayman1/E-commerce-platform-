package com.microservices.pro.order.saga;

public record ReleaseInventoryCommand(String orderId) {}