package com.riceoil.platform.payments.api;

import com.riceoil.platform.payments.application.PaymentOrderService;
import com.riceoil.platform.payments.application.PaymentVerificationService;
import com.riceoil.platform.payments.application.RazorpayCreateOrderResult;
import com.riceoil.platform.payments.application.RazorpayVerifyResult;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/razorpay")
public class RazorpayPaymentController {

    private final PaymentOrderService paymentOrderService;
    private final PaymentVerificationService paymentVerificationService;

    public RazorpayPaymentController(PaymentOrderService paymentOrderService,
                                     PaymentVerificationService paymentVerificationService) {
        this.paymentOrderService = paymentOrderService;
        this.paymentVerificationService = paymentVerificationService;
    }

    @PostMapping("/create-order")
    public RazorpayCreateOrderResponse createOrder(@Valid @RequestBody RazorpayCreateOrderRequest request,
                                                   @AuthenticationPrincipal Jwt jwt,
                                                   @RequestHeader(name = "X-Guest-Order-Token", required = false) String guestOrderToken) {
        RazorpayCreateOrderResult result = paymentOrderService.createRazorpayOrder(
                request.orderId(),
                customerId(jwt),
                guestOrderToken
        );
        return new RazorpayCreateOrderResponse(
                result.orderId(),
                result.providerOrderId(),
                result.currency(),
                result.amount(),
                result.status()
        );
    }

    @PostMapping("/verify")
    public RazorpayVerifyResponse verify(@Valid @RequestBody RazorpayVerifyRequest request,
                                         @AuthenticationPrincipal Jwt jwt,
                                         @RequestHeader(name = "X-Guest-Order-Token", required = false) String guestOrderToken) {
        RazorpayVerifyResult result = paymentVerificationService.verify(
                request.orderId(),
                request.paymentId(),
                request.signature(),
                customerId(jwt),
                guestOrderToken
        );
        return new RazorpayVerifyResponse(
                result.orderId(),
                result.paymentId(),
                result.status(),
                result.verified()
        );
    }

    @PostMapping("/webhook")
    public void webhook(@RequestBody String payload,
                        @RequestHeader("X-Razorpay-Signature") String signature) {
        paymentVerificationService.processWebhook(payload, signature);
    }

    private Long customerId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object customerId = jwt.getClaim("customerId");
        if (customerId instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Authenticated token is missing customer identity");
    }
}
