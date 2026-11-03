package com.riceoil.platform.orders.api.aftercare;

import java.math.BigDecimal;

public record RefundResponseHttp(
        Long id,
        Long orderId,
        BigDecimal amount,
        String status,
        String reason
) {
}
