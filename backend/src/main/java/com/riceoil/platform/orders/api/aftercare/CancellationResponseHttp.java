package com.riceoil.platform.orders.api.aftercare;

public record CancellationResponseHttp(
        Long id,
        Long orderId,
        String status,
        String reason
) {
}
