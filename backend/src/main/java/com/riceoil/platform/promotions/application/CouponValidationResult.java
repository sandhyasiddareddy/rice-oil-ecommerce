package com.riceoil.platform.promotions.application;

import java.math.BigDecimal;
import java.util.List;

public record CouponValidationResult(
        String appliedCode,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        List<String> rejectedCodes
) {
}
