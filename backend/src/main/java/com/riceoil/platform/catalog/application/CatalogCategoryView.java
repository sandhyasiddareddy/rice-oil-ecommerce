package com.riceoil.platform.catalog.application;

public record CatalogCategoryView(
        Long id,
        String name,
        String slug,
        String description
) {
}
