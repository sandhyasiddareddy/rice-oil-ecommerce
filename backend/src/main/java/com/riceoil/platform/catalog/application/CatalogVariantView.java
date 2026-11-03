package com.riceoil.platform.catalog.application;

import java.math.BigDecimal;

public record CatalogVariantView(
        Long id,
        String sku,
        BigDecimal packSize,
        String unit,
        int weightGrams,
        BigDecimal regularPrice,
        String currency
) {
}
