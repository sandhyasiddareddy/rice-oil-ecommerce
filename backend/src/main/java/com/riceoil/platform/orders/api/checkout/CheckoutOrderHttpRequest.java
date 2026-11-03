package com.riceoil.platform.orders.api.checkout;

import jakarta.validation.constraints.NotNull;

public record CheckoutOrderHttpRequest(
        @NotNull Long cartId,
        String customerEmail
) {
}
