package com.microservices.pro.order.saga;

public record ReserveInventoryCommand(String orderId, String productId, int quantity) {}