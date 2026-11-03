package com.riceoil.platform.orders.application.aftercare;

import java.math.BigDecimal;

public record RefundSnapshot(
        Long id,
        Long orderId,
        BigDecimal amount,
        String status,
        String reason
) {
}
