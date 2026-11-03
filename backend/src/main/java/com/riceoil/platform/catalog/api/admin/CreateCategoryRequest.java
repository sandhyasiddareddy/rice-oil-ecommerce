package com.riceoil.platform.catalog.api.admin;

import jakarta.validation.constraints.NotBlank;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        boolean active
) {
}
