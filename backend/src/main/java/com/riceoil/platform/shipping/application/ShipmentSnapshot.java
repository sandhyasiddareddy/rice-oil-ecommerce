package com.riceoil.platform.shipping.application;

import com.riceoil.platform.shipping.domain.ShipmentStatus;

import java.util.List;

public record ShipmentSnapshot(
        Long id,
        Long orderId,
        String shipmentCode,
        String provider,
        String trackingNumber,
        String trackingUrl,
        ShipmentStatus status,
        List<ShipmentEventSnapshot> events
) {
}
