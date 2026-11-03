package com.riceoil.platform.imports.application;

public record ImportApplyResult(
        Long jobId,
        int appliedRows,
        int createdCategories,
        int createdProducts,
        int createdVariants
) {
}
