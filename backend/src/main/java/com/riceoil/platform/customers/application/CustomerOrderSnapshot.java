package com.riceoil.platform.customers.application;

import java.math.BigDecimal;

public record CustomerOrderSnapshot(
        Long id,
        String orderNumber,
        String status,
        String currency,
        BigDecimal totalAmount,
        String customerEmail
) {
}
