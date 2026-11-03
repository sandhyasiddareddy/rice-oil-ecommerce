package com.riceoil.platform.cart.api;

import java.math.BigDecimal;
import java.util.List;

public record ApplyCouponHttpResponse(
        String appliedCoupons,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<String> rejectedCoupons
) {
}
