package com.riceoil.platform.orders.application;

import com.riceoil.platform.orders.domain.GuestOrderAccessTokenEntity;
import com.riceoil.platform.orders.domain.GuestOrderAccessTokenRepository;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class GuestOrderAccessService {

    private final OrderRepository orderRepository;
    private final GuestOrderAccessTokenRepository tokenRepository;

    public GuestOrderAccessService(OrderRepository orderRepository,
                                   GuestOrderAccessTokenRepository tokenRepository) {
        this.orderRepository = orderRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public GuestOrderAccessResult issueToken(Long orderId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        if (order.getCustomerId() != null) {
            throw new IllegalArgumentException("Guest access tokens can only be issued for guest orders");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        GuestOrderAccessTokenEntity entity = new GuestOrderAccessTokenEntity();
        entity.setOrder(order);
        entity.setTokenHash(hash(token));
        entity.setExpiresAt(OffsetDateTime.now().plusDays(30));
        tokenRepository.save(entity);

        return new GuestOrderAccessResult(order.getId(), token, entity.getExpiresAt());
    }

    @Transactional(readOnly = true)
    public OrderStatus verifyAndGetStatus(Long orderId, String token) {
        return verifyAndGetOrder(orderId, token).status();
    }

    @Transactional(readOnly = true)
    public GuestOrderAccessSnapshot verifyAndGetOrder(Long orderId, String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Guest access token is required");
        }
        GuestOrderAccessTokenEntity entity = tokenRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new ResourceNotFoundException("Guest token not found"));
        if (!entity.getOrder().getId().equals(orderId)) {
            throw new IllegalArgumentException("Token does not belong to requested order");
        }
        if (!entity.getExpiresAt().isAfter(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Guest token has expired");
        }
        return new GuestOrderAccessSnapshot(
                entity.getOrder().getId(),
                entity.getOrder().getOrderNumber(),
                entity.getOrder().getStatus()
        );
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash token", ex);
        }
    }
}
