package com.riceoil.platform.cart.application;

import java.math.BigDecimal;

public record CartItemSnapshot(
        Long id,
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
