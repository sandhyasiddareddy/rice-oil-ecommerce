package com.riceoil.platform.cart.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteHttpRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal subtotal,
        String taxCode,
        List<String> couponCodes
) {
}
