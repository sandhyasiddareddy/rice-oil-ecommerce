package com.riceoil.platform.orders.api.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

public record CheckoutOrderHttpRequest(
        @NotNull Long cartId,
        String customerEmail,
        @NotNull @Valid CheckoutAddressHttpRequest shippingAddress,
        @Valid CheckoutAddressHttpRequest billingAddress
) {
}
