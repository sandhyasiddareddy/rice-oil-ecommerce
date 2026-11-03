package com.riceoil.platform.cart.application;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteRequest(
        BigDecimal subtotal,
        String taxCode,
        List<String> couponCodes,
        String customerEmail
) {
}
