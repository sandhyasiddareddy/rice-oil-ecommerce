package com.riceoil.platform.orders.application;

import java.math.BigDecimal;
import java.util.List;

public record OrderLinesSnapshot(List<OrderItemSnapshot> items, BigDecimal shippingAmount) {

    public static OrderLinesSnapshot empty() {
        return new OrderLinesSnapshot(List.of(), BigDecimal.ZERO);
    }
}
