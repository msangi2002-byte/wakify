package com.wakilfly.entity;

public enum PaymentStatus {
    PENDING, // Waiting for payment
    PROCESSING, // Payment is being processed
    SUCCESS, // Payment successful
    FAILED, // Payment failed
    CANCELLED, // Payment cancelled
    REFUNDED // Payment refunded
}
