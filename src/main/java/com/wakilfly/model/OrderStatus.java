package com.wakilfly.model;

public enum OrderStatus {
    DRAFT, // Created from inquiry; seller can amend price/shipping
    PENDING_CONFIRMATION, // Seller sent quote; buyer can accept or seller can amend
    PENDING, // Order placed (cart), awaiting payment/confirmation
    CONFIRMED, // Buyer confirmed (inquiry flow) or order confirmed by seller; ready for payment
    PROCESSING, // Order being prepared
    SHIPPED, // Order shipped
    DELIVERED, // Order delivered
    COMPLETED, // Order completed (alias for delivered)
    CANCELLED, // Order cancelled
    REFUNDED // Order refunded
}
