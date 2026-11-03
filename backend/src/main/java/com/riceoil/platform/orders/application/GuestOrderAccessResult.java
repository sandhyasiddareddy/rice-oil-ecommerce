package com.riceoil.platform.orders.application;

import java.time.OffsetDateTime;

public record GuestOrderAccessResult(Long orderId, String accessToken, OffsetDateTime expiresAt) {
}
