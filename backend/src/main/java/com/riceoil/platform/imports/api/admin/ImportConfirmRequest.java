package com.riceoil.platform.imports.api.admin;

import jakarta.validation.constraints.NotBlank;

public record ImportConfirmRequest(
        @NotBlank String actor
) {
}
