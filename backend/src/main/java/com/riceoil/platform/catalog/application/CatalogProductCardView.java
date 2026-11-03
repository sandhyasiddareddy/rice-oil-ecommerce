package com.riceoil.platform.catalog.application;

import java.math.BigDecimal;

public record CatalogProductCardView(
        Long id,
        String name,
        String slug,
        String categorySlug,
        String description,
        BigDecimal startingPrice,
        String currency
) {
}
