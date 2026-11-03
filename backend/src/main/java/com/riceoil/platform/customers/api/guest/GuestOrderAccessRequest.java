package com.riceoil.platform.customers.api.guest;

import jakarta.validation.constraints.NotNull;

public record GuestOrderAccessRequest(
        @NotNull Long orderId
) {
}
