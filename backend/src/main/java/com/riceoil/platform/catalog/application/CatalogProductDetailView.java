package com.riceoil.platform.catalog.application;

import java.util.List;

public record CatalogProductDetailView(
        Long id,
        String name,
        String slug,
        String description,
        String categorySlug,
        String imageUrl,
        List<CatalogVariantView> variants
) {
}
