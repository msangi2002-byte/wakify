package com.wakilfly.model;

public enum InquiryStatus {
    OPEN,           // Buyer sent; awaiting seller response
    QUOTED,         // Seller sent quote (price/terms)
    ACCEPTED,       // Buyer accepted quote → can convert to order
    REJECTED,       // Buyer or seller closed without order
    CONVERTED_TO_ORDER  // Converted to draft order
}
