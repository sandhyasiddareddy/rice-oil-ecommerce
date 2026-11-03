package com.riceoil.platform.cart.application;

import com.riceoil.platform.pricing.application.PricingQuoteResult;
import com.riceoil.platform.pricing.application.PricingQuoteService;
import com.riceoil.platform.promotions.application.CouponValidationResult;
import com.riceoil.platform.promotions.application.CouponValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckoutQuoteServiceTest {

    @Mock
    private PricingQuoteService pricingQuoteService;

    @Mock
    private CouponValidationService couponValidationService;

    @InjectMocks
    private CheckoutQuoteService checkoutQuoteService;

    @Test
    void quote_shouldComposePricingAndCouponResults() {
        when(pricingQuoteService.quote(new BigDecimal("100.00"), "GST5"))
                .thenReturn(new PricingQuoteResult(
                        new BigDecimal("100.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("105.00"),
                        "GST5",
                        false
                ));

        when(couponValidationService.validateBestEligible(
                eq(new BigDecimal("105.00")),
                eq(List.of("SAVE20")),
                any(),
                isNull()
        ))
                .thenReturn(new CouponValidationResult(
                        "SAVE20",
                        new BigDecimal("20.00"),
                        new BigDecimal("85.00"),
                        List.of()
                ));

        CheckoutQuoteResult result = checkoutQuoteService.quote(
                new CheckoutQuoteRequest(new BigDecimal("100.00"), "GST5", List.of("SAVE20"), null)
        );

        assertEquals(new BigDecimal("100.00"), result.baseAmount());
        assertEquals(new BigDecimal("5.00"), result.taxAmount());
        assertEquals(new BigDecimal("105.00"), result.preDiscountTotal());
        assertEquals("SAVE20", result.appliedCoupon());
        assertEquals(new BigDecimal("20.00"), result.discountAmount());
        assertEquals(new BigDecimal("85.00"), result.finalAmount());
    }

    @Test
    void quote_shouldRejectNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> checkoutQuoteService.quote(null));
    }
}
