package com.riceoil.platform.cart.api;

import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateCartItemRequest(
        BigDecimal unitPrice,
        @Min(1) Integer quantity
) {
}
