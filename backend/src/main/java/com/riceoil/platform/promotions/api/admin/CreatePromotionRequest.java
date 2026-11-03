package com.riceoil.platform.promotions.api.admin;

import com.riceoil.platform.promotions.domain.DiscountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreatePromotionRequest(
        @NotBlank String code,
        @NotNull DiscountType discountType,
        @NotNull BigDecimal discountValue,
        BigDecimal minOrderValue,
        boolean stackable,
        String exclusionGroup,
        Integer maxTotalRedemptions,
        Integer maxRedemptionsPerCustomer,
        boolean active,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt
) {
}
