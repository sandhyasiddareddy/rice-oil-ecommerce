package com.riceoil.platform.cart.api;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
