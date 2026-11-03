package com.riceoil.platform.customers.api.guest;

import java.time.OffsetDateTime;

public record GuestOrderAccessResponse(
        Long orderId,
        String accessToken,
        OffsetDateTime expiresAt
) {
}
