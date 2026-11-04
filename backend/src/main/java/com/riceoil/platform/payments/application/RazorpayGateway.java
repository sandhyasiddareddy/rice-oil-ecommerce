package com.riceoil.platform.payments.application;

import java.util.Map;

public interface RazorpayGateway {

    String createOrder(long amountInSubunits, String currency, String receipt);

    String keyId();

    RazorpayPaymentDetails fetchPayment(String paymentId);

    RazorpayRefundDetails createRefund(String paymentId, long amountInSubunits, String reference);
}
