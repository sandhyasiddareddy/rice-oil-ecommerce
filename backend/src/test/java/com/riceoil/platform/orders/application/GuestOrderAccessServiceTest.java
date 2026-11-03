package com.riceoil.platform.orders.application;

import com.riceoil.platform.orders.domain.GuestOrderAccessTokenEntity;
import com.riceoil.platform.orders.domain.GuestOrderAccessTokenRepository;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GuestOrderAccessServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private GuestOrderAccessTokenRepository tokenRepository;

    @Test
    void issueToken_shouldRejectAccountOrders() throws Exception {
        OrderEntity order = new OrderEntity();
        order.setCustomerId(4L);
        when(orderRepository.findById(12L)).thenReturn(Optional.of(order));

        assertThrows(IllegalArgumentException.class, () -> service().issueToken(12L));
    }

    @Test
    void verifyAndGetOrder_shouldAcceptMatchingUnexpiredToken() throws Exception {
        OrderEntity order = new OrderEntity();
        setField(order, "id", 12L);
        order.setOrderNumber("ORD-12");
        order.setStatus(OrderStatus.PAID);

        GuestOrderAccessService service = service();
        String token = "random-guest-secret";
        String tokenHash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        GuestOrderAccessTokenEntity accessToken = new GuestOrderAccessTokenEntity();
        accessToken.setOrder(order);
        accessToken.setTokenHash(tokenHash);
        accessToken.setExpiresAt(OffsetDateTime.now().plusDays(1));
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(accessToken));

        GuestOrderAccessSnapshot snapshot = service.verifyAndGetOrder(12L, token);

        assertEquals(12L, snapshot.orderId());
        assertEquals("ORD-12", snapshot.orderNumber());
        assertEquals(OrderStatus.PAID, snapshot.status());
    }

    @Test
    void verifyAndGetOrder_shouldRejectExpiredToken() throws Exception {
        OrderEntity order = new OrderEntity();
        setField(order, "id", 12L);
        GuestOrderAccessService service = service();
        String token = "expired-secret";
        String tokenHash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256")
                        .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        GuestOrderAccessTokenEntity accessToken = new GuestOrderAccessTokenEntity();
        accessToken.setOrder(order);
        accessToken.setTokenHash(tokenHash);
        accessToken.setExpiresAt(OffsetDateTime.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(accessToken));

        assertThrows(IllegalArgumentException.class, () -> service.verifyAndGetOrder(12L, token));
    }

    private GuestOrderAccessService service() {
        return new GuestOrderAccessService(orderRepository, tokenRepository);
    }

    private static void setField(Object target, String fieldName, Long value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
