package com.riceoil.platform.payments.application;

public record RazorpayPaymentDetails(
        String id,
        String orderId,
        long amountInSubunits,
        String currency,
        String status,
        boolean captured
) {
}
