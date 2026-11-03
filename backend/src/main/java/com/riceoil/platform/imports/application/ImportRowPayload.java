package com.riceoil.platform.imports.application;

import com.riceoil.platform.catalog.domain.PackUnit;

import java.math.BigDecimal;

public record ImportRowPayload(
        String sku,
        String productName,
        String category,
        String description,
        BigDecimal packSize,
        PackUnit unit,
        BigDecimal regularPrice,
        int stockQuantity,
        String imageUrl,
        boolean published
) {
}
