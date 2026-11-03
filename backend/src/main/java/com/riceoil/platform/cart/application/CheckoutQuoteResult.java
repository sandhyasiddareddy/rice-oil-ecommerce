package com.riceoil.platform.cart.application;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutQuoteResult(
        BigDecimal baseAmount,
        BigDecimal taxAmount,
        BigDecimal preDiscountTotal,
        String taxCode,
        boolean taxInclusive,
        String appliedCoupon,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<String> rejectedCoupons
) {
}
