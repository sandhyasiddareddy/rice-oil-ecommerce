package com.riceoil.platform.inventory.api.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdjustInventoryRequest(
        @NotNull Long variantId,
        int deltaQty,
        @NotBlank String reason,
        String reference,
        @NotBlank String createdBy
) {
}
