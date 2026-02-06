package com.wakilfly.entity;

public enum OrderStatus {
    PENDING, // Order placed, awaiting payment/confirmation
    CONFIRMED, // Order confirmed by seller
    PROCESSING, // Order being prepared
    SHIPPED, // Order shipped
    DELIVERED, // Order delivered
    CANCELLED, // Order cancelled
    REFUNDED // Order refunded
}
