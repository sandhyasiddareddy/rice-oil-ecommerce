package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.domain.OrderStatus;

public record RazorpayVerifyResult(
        Long orderId,
        String paymentId,
        OrderStatus status,
        boolean verified
) {
}
