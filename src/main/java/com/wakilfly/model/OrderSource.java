package com.wakilfly.model;

/**
 * How the order was created: cart (direct checkout) or inquiry (RFQ/negotiation flow).
 */
public enum OrderSource {
    CART,
    INQUIRY
}
