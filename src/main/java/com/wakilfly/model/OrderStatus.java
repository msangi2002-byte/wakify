package com.wakilfly.model;

public enum OrderStatus {
    PENDING, // Order placed, awaiting payment/confirmation
    CONFIRMED, // Order confirmed by seller
    PROCESSING, // Order being prepared
    SHIPPED, // Order shipped
    DELIVERED, // Order delivered
    COMPLETED, // Order completed (alias for delivered)
    CANCELLED, // Order cancelled
    REFUNDED // Order refunded
}
