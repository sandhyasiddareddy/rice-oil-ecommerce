package com.riceoil.platform.cart.api;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record ApplyCouponHttpRequest(
        @NotNull BigDecimal subtotal,
        String taxCode,
        List<String> couponCodes,
        String customerEmail
) {
}
