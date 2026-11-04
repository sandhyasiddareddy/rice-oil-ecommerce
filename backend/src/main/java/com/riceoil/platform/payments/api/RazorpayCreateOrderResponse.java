package com.riceoil.platform.payments.api;

import com.riceoil.platform.orders.domain.OrderStatus;

public record RazorpayCreateOrderResponse(
        Long orderId,
        String providerOrderId,
        String currency,
        long amount,
        OrderStatus status,
        String keyId
) {
}
