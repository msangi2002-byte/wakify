package com.wakilfly.model;

public enum CommissionStatus {
    PENDING, // Waiting for approval/payment
    APPROVED, // Approved, ready for payout
    PAID, // Paid to agent
    CANCELLED // Cancelled
}
