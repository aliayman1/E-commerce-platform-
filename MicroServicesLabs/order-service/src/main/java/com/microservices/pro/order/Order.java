package com.microservices.pro.order;

import java.math.BigDecimal;

public class Order {

    private final String orderId;
    private final String productId;
    private final int quantity;
    private final BigDecimal amount;
    private final String customerId;
    private OrderStatus status;

    public Order(String orderId, String productId, int quantity, BigDecimal amount,
                 String customerId, OrderStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.amount = amount;
        this.customerId = customerId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}