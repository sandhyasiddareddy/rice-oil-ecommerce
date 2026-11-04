package com.riceoil.platform.orders.application;

import com.riceoil.platform.orders.domain.OrderStatus;

import java.math.BigDecimal;

public record GuestOrderAccessSnapshot(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        String currency,
        BigDecimal totalAmount
) {
}
