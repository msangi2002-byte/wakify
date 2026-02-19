package com.wakilfly.model;

/** Admin panel areas for RBAC. */
public enum AdminArea {
    DASHBOARD,
    DASHBOARD_CHARTS,
    MAP,
    MEDIA_STATS,
    TRANSACTION_REPORTS,
    ANALYTICS,
    AUDIENCE_ANALYTICS,
    EXPORT_USERS,
    EXPORT_BUSINESSES,
    USERS,
    BUSINESSES,
    AGENTS,
    PRODUCTS,
    ORDERS,
    PAYMENTS,
    WITHDRAWALS,
    USER_WITHDRAWALS,
    REPORTS,
    PROMOTIONS,
    AUDIT_LOGS,
    SETTINGS,
    AGENT_PACKAGES,
    /** Business registration subscription plans (fees for "Become a business"). */
    BUSINESS_REGISTRATION_PLANS,
    IMPERSONATE,
    /** Manage role definitions (add/edit/delete roles and their areas). Super Admin only. */
    ROLE_DEFINITIONS,
}
