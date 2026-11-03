package com.riceoil.platform.cart.application;

import java.math.BigDecimal;

public record AddCartItemCommand(
        String sku,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}
