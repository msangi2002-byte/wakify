package com.wakilfly.model;

/**
 * Granular admin roles (RBAC). Only applies when user.role == ADMIN.
 * Null or missing = SUPER_ADMIN for backward compatibility.
 */
public enum AdminRole {
    /** Full access: dashboard, all sections, settings, impersonate, exports. */
    SUPER_ADMIN,

    /** Content & moderation only: Reports, Content Queue. No revenue/payments. */
    MODERATOR,

    /** Support: Users and Orders only. No system settings. */
    SUPPORT_AGENT,

    /** Finance: Withdrawals and Payments only. */
    FINANCE_MANAGER
}
