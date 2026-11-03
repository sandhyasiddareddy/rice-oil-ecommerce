package com.riceoil.platform.cart.api;

import java.util.List;

public record CartPricingRefreshRequest(
        String taxCode,
        List<String> couponCodes
) {
}
