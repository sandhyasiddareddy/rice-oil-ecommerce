package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.domain.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long id,
        CartStatus status,
        String currency,
        String taxCode,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<String> couponCodes,
        List<CartItemResponse> items,
        String guestAccessToken
) {
}
