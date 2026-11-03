package com.riceoil.platform.cart.application;

import com.riceoil.platform.cart.domain.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartSnapshot(
        Long id,
        CartStatus status,
        String currency,
        String taxCode,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        List<String> couponCodes,
        List<CartItemSnapshot> items,
        String guestAccessToken
) {
    public CartSnapshot(Long id, CartStatus status, String currency, String taxCode,
                        BigDecimal subtotalAmount, BigDecimal taxAmount, BigDecimal discountAmount,
                        BigDecimal totalAmount, List<String> couponCodes, List<CartItemSnapshot> items) {
        this(id, status, currency, taxCode, subtotalAmount, taxAmount, discountAmount, totalAmount,
                couponCodes, items, null);
    }
}
