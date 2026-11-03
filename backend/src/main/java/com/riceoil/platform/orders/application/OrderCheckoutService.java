package com.riceoil.platform.orders.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.domain.CartStatus;
import com.riceoil.platform.cart.application.GuestCartAccessToken;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.promotions.application.PromotionRedemptionService;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderCheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final PromotionRedemptionService promotionRedemptionService;
    private final ObjectMapper objectMapper;

    public OrderCheckoutService(CartRepository cartRepository,
                                CartItemRepository cartItemRepository,
                                OrderRepository orderRepository,
                                PromotionRedemptionService promotionRedemptionService,
                                ObjectMapper objectMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.promotionRedemptionService = promotionRedemptionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderSnapshot placeOrder(String idempotencyKey, PlaceOrderCommand command) {
        return placeOrder(idempotencyKey, command, null, null);
    }

    @Transactional
    public OrderSnapshot placeOrder(String idempotencyKey, PlaceOrderCommand command, Long authenticatedCustomerId) {
        return placeOrder(idempotencyKey, command, authenticatedCustomerId, null);
    }

    @Transactional
    public OrderSnapshot placeOrder(String idempotencyKey, PlaceOrderCommand command, Long authenticatedCustomerId,
                                    String guestCartAccessToken) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);

        Optional<OrderEntity> existing = orderRepository.findByIdempotencyKey(normalizedKey);
        if (existing.isPresent()) {
            OrderEntity existingOrder = existing.get();
            if (existingOrder.getCustomerId() != null) {
                if (!Objects.equals(authenticatedCustomerId, existingOrder.getCustomerId())) {
                    throw new AccessDeniedException("Order does not belong to the authenticated customer");
                }
            } else {
                if (authenticatedCustomerId != null) {
                    throw new AccessDeniedException("Order does not belong to the authenticated customer");
                }
                if (!GuestCartAccessToken.matches(
                            guestCartAccessToken,
                            existingOrder.getCart().getGuestAccessTokenHash()
                    )) {
                    throw new AccessDeniedException("Guest cart token is invalid");
                }
            }
            return toSnapshot(existingOrder);
        }

        CartEntity cart = cartRepository.findById(command.cartId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + command.cartId()));
        if (cart.getCustomerId() != null) {
            if (authenticatedCustomerId == null || !cart.getCustomerId().equals(authenticatedCustomerId)) {
                throw new AccessDeniedException("Cart does not belong to the authenticated customer");
            }
        } else if (authenticatedCustomerId != null
                || !GuestCartAccessToken.matches(guestCartAccessToken, cart.getGuestAccessTokenHash())) {
            throw new AccessDeniedException("Guest cart token is invalid");
        }

        if (cart.getStatus() != CartStatus.ACTIVE) {
            throw new IllegalArgumentException("Cart is not active and cannot be checked out");
        }
        if (cartItemRepository.findByCartIdOrderByIdAsc(cart.getId()).isEmpty()) {
            throw new IllegalArgumentException("Cart has no items");
        }

        OrderEntity order = new OrderEntity();
        order.setOrderNumber(generateOrderNumber());
        order.setCart(cart);
        order.setCustomerId(authenticatedCustomerId);
        order.setIdempotencyKey(normalizedKey);
        order.setStatus(OrderStatus.CREATED);
        order.setCurrency(cart.getCurrency());
        order.setSubtotalAmount(cart.getSubtotalAmount());
        order.setTaxAmount(cart.getTaxAmount());
        order.setDiscountAmount(cart.getDiscountAmount());
        order.setTotalAmount(cart.getTotalAmount());
        order.setCustomerEmail(normalizeEmail(command.customerEmail()));

        OrderEntity saved = orderRepository.save(order);

        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        promotionRedemptionService.registerAppliedRedemptions(
                preDiscountSubtotal(cart),
                readCouponCodes(cart.getCouponCodesJson()),
                saved.getCustomerEmail(),
                OffsetDateTime.now()
        );

        return toSnapshot(saved);
    }

    private BigDecimal preDiscountSubtotal(CartEntity cart) {
        return cart.getSubtotalAmount().add(cart.getTaxAmount());
    }

    private List<String> readCouponCodes(String couponCodesJson) {
        if (couponCodesJson == null || couponCodesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(couponCodesJson, new TypeReference<ArrayList<String>>() {
            });
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key header is required");
        }
        String normalized = key.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Idempotency-Key must be <= 120 chars");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String generateOrderNumber() {
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(OffsetDateTime.now());
        String random = Long.toUnsignedString(Double.doubleToLongBits(Math.random()), 36)
                .toUpperCase(Locale.ROOT)
                .substring(0, 6);
        return "ORD-" + stamp + "-" + random;
    }

    private OrderSnapshot toSnapshot(OrderEntity order) {
        return new OrderSnapshot(
                order.getId(),
                order.getOrderNumber(),
                order.getCart().getId(),
                order.getStatus(),
                order.getCurrency(),
                order.getSubtotalAmount(),
                order.getTaxAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCustomerEmail()
        );
    }
}
