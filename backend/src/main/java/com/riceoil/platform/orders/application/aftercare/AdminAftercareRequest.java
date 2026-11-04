package com.riceoil.platform.orders.application.aftercare;

import java.math.BigDecimal;

public record AdminAftercareRequest(
        Long id,
        Long orderId,
        String orderNumber,
        String customerEmail,
        BigDecimal amount,
        String status,
        String reason,
        String providerRefundId,
        String providerStatus
) {
}
