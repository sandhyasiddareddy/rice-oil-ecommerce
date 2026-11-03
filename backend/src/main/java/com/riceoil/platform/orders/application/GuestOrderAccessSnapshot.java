package com.riceoil.platform.orders.application;

import com.riceoil.platform.orders.domain.OrderStatus;

public record GuestOrderAccessSnapshot(Long orderId, String orderNumber, OrderStatus status) {
}
