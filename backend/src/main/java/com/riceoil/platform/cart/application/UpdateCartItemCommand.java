package com.riceoil.platform.cart.application;

import java.math.BigDecimal;

public record UpdateCartItemCommand(
        BigDecimal unitPrice,
        Integer quantity
) {
}
