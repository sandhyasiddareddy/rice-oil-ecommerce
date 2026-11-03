package com.riceoil.platform.payments.api;

import com.riceoil.platform.orders.domain.OrderStatus;

public record RazorpayVerifyResponse(
        Long orderId,
        String paymentId,
        OrderStatus status,
        boolean verified
) {
}
