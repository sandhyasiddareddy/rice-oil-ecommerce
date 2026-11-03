package com.riceoil.platform.shipping.domain;

public enum ShipmentStatus {
    SHIPMENT_CREATED,
    PICKED_UP,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    EXCEPTION,
    CANCELLED
}
