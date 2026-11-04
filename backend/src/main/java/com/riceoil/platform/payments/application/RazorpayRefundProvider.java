package com.riceoil.platform.payments.application;

import com.riceoil.platform.orders.application.aftercare.RefundProvider;
import org.springframework.stereotype.Component;

@Component
public class RazorpayRefundProvider implements RefundProvider {

    private final RazorpayGateway razorpayGateway;

    public RazorpayRefundProvider(RazorpayGateway razorpayGateway) {
        this.razorpayGateway = razorpayGateway;
    }

    @Override
    public ProviderRefundResult refund(String paymentId, long amountInSubunits, String reference) {
        RazorpayRefundDetails result = razorpayGateway.createRefund(paymentId, amountInSubunits, reference);
        return new ProviderRefundResult(result.id(), result.status());
    }
}
