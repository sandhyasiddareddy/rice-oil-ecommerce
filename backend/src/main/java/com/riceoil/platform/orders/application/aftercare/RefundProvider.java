package com.riceoil.platform.orders.application.aftercare;

public interface RefundProvider {

    ProviderRefundResult refund(String paymentId, long amountInSubunits, String reference);

    record ProviderRefundResult(String refundId, String status) {
    }
}
