package com.riceoil.platform.orders.application;

public record PlaceOrderCommand(
        Long cartId,
        String customerEmail,
        OrderAddressSnapshot shippingAddress,
        OrderAddressSnapshot billingAddress
) {
    public PlaceOrderCommand(Long cartId, String customerEmail) {
        this(cartId, customerEmail, null, null);
    }
}
