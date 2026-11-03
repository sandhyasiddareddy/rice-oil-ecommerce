package com.riceoil.platform.orders.api.checkout;

import com.riceoil.platform.orders.domain.OrderStatus;

import java.math.BigDecimal;

public record CheckoutOrderHttpResponse(
        Long id,
        String orderNumber,
        Long cartId,
        OrderStatus status,
        String currency,
        BigDecimal subtotalAmount,
        BigDecimal taxAmount,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        String customerEmail,
        String guestAccessToken
) {
}
