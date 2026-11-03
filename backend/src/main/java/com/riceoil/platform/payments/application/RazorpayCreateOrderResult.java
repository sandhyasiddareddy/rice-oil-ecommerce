package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderStatus;

import java.math.BigDecimal;

public record RazorpayCreateOrderResult(
        Long orderId,
        String providerOrderId,
        String currency,
        BigDecimal amount,
        OrderStatus status
) {
}
