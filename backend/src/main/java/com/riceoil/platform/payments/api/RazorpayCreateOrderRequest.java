package com.riceoil.platform.payments.api;

import jakarta.validation.constraints.NotNull;

public record RazorpayCreateOrderRequest(
        @NotNull Long orderId
) {
}
