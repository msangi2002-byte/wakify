package com.wakilfly.model;

/**
 * Order tracking steps for map/status display (Alibaba-style).
 */
public enum TrackingEventType {
    AT_STORE,       // At seller's store
    PACKAGING,      // Being packed
    SHIPPED,        // Left store / handed to courier
    IN_TRANSIT,     // On the way (driving / flight)
    DELIVERED       // Reached buyer
}
