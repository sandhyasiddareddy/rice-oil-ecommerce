package com.riceoil.platform.payments.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RazorpayWebhookRequest(
        @NotBlank String eventId,
        @NotBlank String eventType,
        @NotNull Long orderId,
        @NotBlank String paymentId,
        String payload
) {
}
