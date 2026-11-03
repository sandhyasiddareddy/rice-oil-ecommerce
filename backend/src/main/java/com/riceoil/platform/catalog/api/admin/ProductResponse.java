package com.riceoil.platform.catalog.api.admin;

public record ProductResponse(
        Long id,
        Long categoryId,
        String name,
        String slug,
        String description,
        String attributesJson,
        boolean published
) {
}
