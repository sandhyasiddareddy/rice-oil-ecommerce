package com.riceoil.platform.shipping.application;

import com.riceoil.platform.shipping.domain.ShipmentStatus;

import java.time.OffsetDateTime;

public record ShipmentEventSnapshot(
        ShipmentStatus status,
        String description,
        OffsetDateTime occurredAt
) {
}
