package com.riceoil.platform.cart.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AddCartItemRequest(
        @NotBlank String sku,
        @NotBlank String productName,
        @NotNull @DecimalMin(value = "0.00") BigDecimal unitPrice,
        @Min(1) int quantity
) {
}
