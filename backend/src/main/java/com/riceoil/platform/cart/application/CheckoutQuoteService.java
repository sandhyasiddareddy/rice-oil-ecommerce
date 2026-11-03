package com.riceoil.platform.cart.application;

import com.riceoil.platform.pricing.application.PricingQuoteResult;
import com.riceoil.platform.pricing.application.PricingQuoteService;
import com.riceoil.platform.promotions.application.CouponValidationResult;
import com.riceoil.platform.promotions.application.CouponValidationService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CheckoutQuoteService {

    private final PricingQuoteService pricingQuoteService;
    private final CouponValidationService couponValidationService;

    public CheckoutQuoteService(PricingQuoteService pricingQuoteService,
                                CouponValidationService couponValidationService) {
        this.pricingQuoteService = pricingQuoteService;
        this.couponValidationService = couponValidationService;
    }

    public CheckoutQuoteResult quote(CheckoutQuoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }

        PricingQuoteResult pricing = pricingQuoteService.quote(request.subtotal(), request.taxCode());
        List<String> codes = request.couponCodes() == null ? List.of() : request.couponCodes();

        CouponValidationResult coupon = couponValidationService.validateBestEligible(
                pricing.totalAmount(),
                codes,
            OffsetDateTime.now(),
            request.customerEmail()
        );

        return new CheckoutQuoteResult(
                pricing.baseAmount(),
                pricing.taxAmount(),
                pricing.totalAmount(),
                pricing.taxCode(),
                pricing.taxInclusive(),
                coupon.appliedCode(),
                coupon.discountAmount(),
                coupon.finalAmount(),
                coupon.rejectedCodes()
        );
    }
}
