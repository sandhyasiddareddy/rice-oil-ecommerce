package com.riceoil.platform.promotions.api.admin;

import com.riceoil.platform.promotions.domain.DiscountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PromotionResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal minOrderValue,
        boolean stackable,
        String exclusionGroup,
        Integer maxTotalRedemptions,
        Integer maxRedemptionsPerCustomer,
        Integer redeemedCount,
        boolean active,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
