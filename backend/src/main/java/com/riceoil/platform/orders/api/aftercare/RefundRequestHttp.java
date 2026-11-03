package com.riceoil.platform.orders.api.aftercare;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RefundRequestHttp(
        @NotNull BigDecimal amount,
        String reason
) {
}
