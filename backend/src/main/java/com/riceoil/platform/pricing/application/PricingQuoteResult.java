package com.riceoil.platform.pricing.application;

import java.math.BigDecimal;

public record PricingQuoteResult(
        BigDecimal baseAmount,
        BigDecimal taxAmount,
        BigDecimal totalAmount,
        String taxCode,
        boolean taxInclusive
) {
}
