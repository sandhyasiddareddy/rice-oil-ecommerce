package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.domain.PackUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVariantRequest(
        @NotNull Long productId,
        @NotBlank String sku,
        @NotNull BigDecimal packSize,
        @NotNull PackUnit unit,
        int weightGrams,
        @NotNull BigDecimal regularPrice,
        boolean active
) {
}
