package com.riceoil.platform.orders.application.aftercare;

public record CancellationSnapshot(
        Long id,
        Long orderId,
        String status,
        String reason
) {
}
