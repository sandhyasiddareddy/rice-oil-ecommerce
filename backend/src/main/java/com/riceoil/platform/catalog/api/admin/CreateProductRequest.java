package com.riceoil.platform.catalog.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProductRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        String attributesJson,
        boolean published
) {
}
