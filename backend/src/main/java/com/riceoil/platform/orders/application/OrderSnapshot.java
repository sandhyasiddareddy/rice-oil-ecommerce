package com.riceoil.platform.orders.application;

import com.riceoil.platform.orders.domain.OrderStatus;

import java.math.BigDecimal;

public record OrderSnapshot(
        Long id,
        String orderNumber,
        Long cartId,
        OrderStatus status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal shippingAmount,
        BigDecimal totalAmount,
        String customerEmail
) {
}
