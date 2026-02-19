package com.wakilfly.model;

public enum BusinessRequestStatus {
    /** Waiting for user to pay. */
    PENDING,
    /** User paid; when agent assigned: waiting for agent to visit and approve. */
    PAID,
    APPROVED,
    REJECTED,
    /** Business created; user is now BUSINESS. */
    CONVERTED
}
