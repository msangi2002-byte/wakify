package com.wakilfly.entity;

public enum Role {
    VISITOR, // Can only view public content
    USER, // Normal user: post, like, comment, follow
    BUSINESS, // Business owner: products, orders, promotions
    AGENT, // Registers/activates businesses, earns commissions
    ADMIN // Full system control
}
