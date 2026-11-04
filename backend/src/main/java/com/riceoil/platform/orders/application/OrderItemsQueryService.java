package com.riceoil.platform.orders.application;

import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.orders.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderItemsQueryService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    public OrderItemsQueryService(OrderRepository orderRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
    }

    /** Order lines are the items of the cart the order was placed from. */
    @Transactional(readOnly = true)
    public OrderLinesSnapshot linesFor(Long orderId) {
        return orderRepository.findById(orderId)
                .map(order -> new OrderLinesSnapshot(
                        cartItemRepository.findByCartIdOrderByIdAsc(order.getCart().getId()).stream()
                                .map(item -> new OrderItemSnapshot(
                                        item.getSku(),
                                        item.getProductName(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getLineTotal()))
                                .toList(),
                        order.getShippingAmount()))
                .orElse(OrderLinesSnapshot.empty());
    }
}
