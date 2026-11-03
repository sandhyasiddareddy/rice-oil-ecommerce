package com.riceoil.platform.shipping.api;

import jakarta.validation.constraints.NotNull;

public record ShippingQuoteRequest(
        @NotNull Long cartId
) {
}
