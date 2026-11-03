package com.riceoil.platform.catalog.api.admin;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        boolean active
) {
}
