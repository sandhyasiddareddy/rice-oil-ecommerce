package com.riceoil.platform.shipping.api;

import com.riceoil.platform.shipping.domain.ShippingQuoteStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ShippingQuoteResponse(
        String quoteCode,
        Long cartId,
        ShippingQuoteStatus status,
        BigDecimal shippingFee,
        String currency,
        String provider,
        String serviceLevel,
        OffsetDateTime expiresAt
) {
}
