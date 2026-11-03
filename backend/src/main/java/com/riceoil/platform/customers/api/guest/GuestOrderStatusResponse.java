package com.riceoil.platform.customers.api.guest;

import com.riceoil.platform.shipping.application.ShipmentSnapshot;

import java.util.List;

public record GuestOrderStatusResponse(
        Long orderId,
        String orderNumber,
        String status,
        List<ShipmentSnapshot> shipments
) {
}
