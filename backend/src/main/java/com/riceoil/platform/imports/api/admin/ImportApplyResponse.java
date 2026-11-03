package com.riceoil.platform.imports.api.admin;

public record ImportApplyResponse(
        Long jobId,
        int appliedRows,
        int createdCategories,
        int createdProducts,
        int createdVariants
) {
}
