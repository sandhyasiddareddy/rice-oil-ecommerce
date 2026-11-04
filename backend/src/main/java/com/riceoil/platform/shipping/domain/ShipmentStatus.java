package com.riceoil.platform.shipping.domain;

public enum ShipmentStatus {
    SHIPMENT_CREATED,
    PACKED,
    SHIPPED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION,
    CANCELLED;

    /** True once the parcel has left the warehouse; customers can no longer cancel from here. */
    public boolean isDispatched() {
        return this == SHIPPED || this == PICKED_UP || this == IN_TRANSIT
                || this == OUT_FOR_DELIVERY || this == DELIVERED;
    }
}
