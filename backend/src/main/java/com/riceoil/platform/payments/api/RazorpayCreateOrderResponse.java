package com.riceoil.platform.payments.api;

import com.riceoil.platform.orders.domain.OrderStatus;

import java.math.BigDecimal;

public record RazorpayCreateOrderResponse(
        Long orderId,
        String providerOrderId,
        String currency,
        BigDecimal amount,
        OrderStatus status
) {
}
