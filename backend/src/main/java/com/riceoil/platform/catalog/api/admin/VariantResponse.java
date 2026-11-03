package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.domain.PackUnit;

import java.math.BigDecimal;

public record VariantResponse(
        Long id,
        Long productId,
        String sku,
        BigDecimal packSize,
        PackUnit unit,
        int weightGrams,
        BigDecimal regularPrice,
        String currency,
        boolean active
) {
}
