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
@RequestMapping("/api/checkout")
public class CheckoutQuoteController {

    private final CheckoutQuoteService checkoutQuoteService;

    public CheckoutQuoteController(CheckoutQuoteService checkoutQuoteService) {
        this.checkoutQuoteService = checkoutQuoteService;
    }

    @PostMapping("/quote")
    public CheckoutQuoteHttpResponse quote(@Valid @RequestBody CheckoutQuoteHttpRequest request) {
        CheckoutQuoteResult result = checkoutQuoteService.quote(
            new CheckoutQuoteRequest(request.subtotal(), request.taxCode(), request.couponCodes(), null)
        );

        return new CheckoutQuoteHttpResponse(
                result.baseAmount(),
                result.taxAmount(),
                result.preDiscountTotal(),
                result.taxCode(),
                result.taxInclusive(),
                result.appliedCoupon(),
                result.discountAmount(),
                result.finalAmount(),
                result.rejectedCoupons()
        );
    }
}
