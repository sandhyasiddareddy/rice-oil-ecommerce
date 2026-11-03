package com.riceoil.platform.orders.application;

public record PlaceOrderCommand(
        Long cartId,
        String customerEmail
) {
}
