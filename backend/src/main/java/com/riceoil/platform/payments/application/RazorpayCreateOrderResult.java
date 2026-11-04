package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderStatus;

public record RazorpayCreateOrderResult(
        Long orderId,
        String providerOrderId,
        String currency,
        long amount,
        OrderStatus status,
        String keyId
) {
}
