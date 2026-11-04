package com.riceoil.platform.orders.application;

import java.math.BigDecimal;

public record OrderItemSnapshot(
        String sku,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
