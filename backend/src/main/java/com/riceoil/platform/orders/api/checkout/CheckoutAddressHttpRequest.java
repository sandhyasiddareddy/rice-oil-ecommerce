package com.riceoil.platform.orders.api.checkout;

import com.riceoil.platform.orders.application.OrderAddressSnapshot;
import jakarta.validation.constraints.NotBlank;

public record CheckoutAddressHttpRequest(
        @NotBlank String fullName,
        @NotBlank String phone,
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        @NotBlank String state,
        @NotBlank String postalCode,
        @NotBlank String country
) {
    public OrderAddressSnapshot toSnapshot() {
        return new OrderAddressSnapshot(
                fullName.trim(),
                phone.trim(),
                line1.trim(),
                line2 == null || line2.isBlank() ? null : line2.trim(),
                city.trim(),
                state.trim(),
                postalCode.trim(),
                country.trim().toUpperCase(java.util.Locale.ROOT)
        );
    }
}
