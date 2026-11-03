package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.application.CheckoutQuoteRequest;
import com.riceoil.platform.cart.application.CheckoutQuoteResult;
import com.riceoil.platform.cart.application.CheckoutQuoteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout/coupons")
public class CheckoutCouponController {

    private final CheckoutQuoteService checkoutQuoteService;

    public CheckoutCouponController(CheckoutQuoteService checkoutQuoteService) {
        this.checkoutQuoteService = checkoutQuoteService;
    }

    @PostMapping("/apply")
    public ApplyCouponHttpResponse apply(@Valid @RequestBody ApplyCouponHttpRequest request) {
        CheckoutQuoteResult result = checkoutQuoteService.quote(
                new CheckoutQuoteRequest(
                        request.subtotal(),
                        request.taxCode(),
                        request.couponCodes(),
                        request.customerEmail()
                )
        );

        return new ApplyCouponHttpResponse(
                result.appliedCoupon(),
                result.discountAmount(),
                result.finalAmount(),
                result.rejectedCoupons()
        );
    }
}
