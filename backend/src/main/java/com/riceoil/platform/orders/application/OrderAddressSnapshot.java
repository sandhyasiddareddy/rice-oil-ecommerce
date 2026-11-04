package com.riceoil.platform.orders.application;

public record OrderAddressSnapshot(
        String fullName,
        String phone,
        String line1,
        String line2,
        String city,
        String state,
        String postalCode,
        String country
) {
}
