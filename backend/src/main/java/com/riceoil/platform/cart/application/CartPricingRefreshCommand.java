package com.riceoil.platform.cart.application;

import java.util.List;

public record CartPricingRefreshCommand(
        String taxCode,
        List<String> couponCodes
) {
}
