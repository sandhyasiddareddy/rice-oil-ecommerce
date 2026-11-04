package com.riceoil.platform.orders.application.aftercare;

import java.math.BigDecimal;

public record AftercareSummary(boolean canRequestCancellation,
                               String cancellationStatus,
                               String refundStatus,
                               BigDecimal refundAmount) {
}
