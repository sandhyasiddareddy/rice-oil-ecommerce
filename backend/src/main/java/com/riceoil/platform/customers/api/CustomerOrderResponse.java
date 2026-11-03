package com.riceoil.platform.customers.api;

import java.math.BigDecimal;

public record CustomerOrderResponse(
        Long id,
        String orderNumber,
        String status,
        String currency,
        BigDecimal totalAmount,
        String customerEmail
) {
}
