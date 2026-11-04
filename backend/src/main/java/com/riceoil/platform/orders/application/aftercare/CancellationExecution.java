package com.riceoil.platform.orders.application.aftercare;

public record CancellationExecution(
        Long cancellationRequestId,
        Long orderId,
        RefundExecution refund,
        boolean alreadyCompleted
) {
}
