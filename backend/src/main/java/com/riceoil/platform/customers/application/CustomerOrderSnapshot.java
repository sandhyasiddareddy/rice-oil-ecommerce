package com.riceoil.platform.customers.application;

import com.riceoil.platform.orders.application.OrderItemSnapshot;
import com.riceoil.platform.shipping.application.ShipmentSnapshot;

import java.math.BigDecimal;
import java.util.List;

public record CustomerOrderSnapshot(
        Long id,
        String orderNumber,
        String status,
        String currency,
        BigDecimal totalAmount,
        BigDecimal shippingAmount,
        List<OrderItemSnapshot> items,
        List<ShipmentSnapshot> shipments,
        String customerEmail,
        boolean canRequestCancellation,
        String cancellationStatus,
        String refundStatus,
        BigDecimal refundAmount
) {
}
