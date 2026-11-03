package com.riceoil.platform.payments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RazorpayVerifyRequest(
        @NotNull Long orderId,
        @NotBlank String paymentId,
        @NotBlank String signature
) {
}
