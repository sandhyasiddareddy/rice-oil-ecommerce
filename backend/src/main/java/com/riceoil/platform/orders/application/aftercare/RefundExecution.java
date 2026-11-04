package com.riceoil.platform.orders.application.aftercare;

import java.math.BigDecimal;

public record RefundExecution(
        Long refundRequestId,
        Long orderId,
        String paymentId,
        BigDecimal amount
) {
    public long amountInSubunits() {
        return amount.movePointRight(2).longValueExact();
    }
}
