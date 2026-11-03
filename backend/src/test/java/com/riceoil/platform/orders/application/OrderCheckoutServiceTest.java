package com.riceoil.platform.orders.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartItemEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.domain.CartStatus;
import com.riceoil.platform.cart.application.GuestCartAccessToken;
import com.riceoil.platform.orders.domain.OrderEntity;
import com.riceoil.platform.orders.domain.OrderRepository;
import com.riceoil.platform.orders.domain.OrderStatus;
import com.riceoil.platform.promotions.application.PromotionRedemptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderCheckoutServiceTest {

    private static final String GUEST_CART_TOKEN = "guest-cart-test-token";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PromotionRedemptionService promotionRedemptionService;

    private OrderCheckoutService orderCheckoutService;

    @BeforeEach
    void setUp() {
        this.orderCheckoutService = new OrderCheckoutService(
                cartRepository,
                cartItemRepository,
                orderRepository,
                promotionRedemptionService,
                new ObjectMapper()
        );
    }

    @Test
    void placeOrder_shouldCreateOrderFromCart() throws Exception {
        CartEntity cart = activeCart(10L);
        CartItemEntity item = new CartItemEntity();
        setPrivateId(item, 99L);
        item.setCart(cart);
        item.setSku("RICE-5KG");
        item.setProductName("Rice");
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("200.00"));

        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(item));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            setPrivateId(entity, 77L);
            return entity;
        });
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderSnapshot result = orderCheckoutService.placeOrder(
                "idem-1",
                new PlaceOrderCommand(10L, "Buyer@Example.com"),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(77L, result.id());
        assertEquals(10L, result.cartId());
        assertEquals(OrderStatus.CREATED, result.status());
        assertEquals("buyer@example.com", result.customerEmail());
    }

    @Test
    void placeOrder_shouldReturnExistingOrderForSameIdempotencyKey() throws Exception {
        CartEntity cart = activeCart(10L);
        OrderEntity existing = new OrderEntity();
        setPrivateId(existing, 5L);
        existing.setOrderNumber("ORD-EXISTING");
        existing.setCart(cart);
        existing.setIdempotencyKey("idem-1");
        existing.setStatus(OrderStatus.CREATED);
        existing.setCurrency("INR");
        existing.setSubtotalAmount(new BigDecimal("100.00"));
        existing.setTaxAmount(new BigDecimal("5.00"));
        existing.setDiscountAmount(new BigDecimal("0.00"));
        existing.setTotalAmount(new BigDecimal("105.00"));

        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        OrderSnapshot result = orderCheckoutService.placeOrder(
                "idem-1",
                new PlaceOrderCommand(10L, "buyer@example.com"),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(5L, result.id());
        assertEquals("ORD-EXISTING", result.orderNumber());
    }

    @Test
    void placeOrder_shouldRequireGuestCartTokenForIdempotentRetry() throws Exception {
        CartEntity cart = activeCart(10L);
        OrderEntity existing = new OrderEntity();
        existing.setCart(cart);
        existing.setIdempotencyKey("idem-guest");
        existing.setStatus(OrderStatus.CREATED);
        existing.setCurrency("INR");
        existing.setSubtotalAmount(new BigDecimal("100.00"));
        existing.setTaxAmount(new BigDecimal("5.00"));
        existing.setDiscountAmount(new BigDecimal("0.00"));
        existing.setTotalAmount(new BigDecimal("105.00"));
        when(orderRepository.findByIdempotencyKey("idem-guest")).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderCheckoutService.placeOrder(
                        "idem-guest",
                        new PlaceOrderCommand(10L, "buyer@example.com"),
                        null,
                        "different-guest-cart-token"
                ));
    }

    @Test
    void placeOrder_shouldLinkAuthenticatedCustomerToCartAndOrder() throws Exception {
        CartEntity cart = activeCart(10L);
        cart.setCustomerId(9L);
        CartItemEntity item = new CartItemEntity();
        setPrivateId(item, 99L);
        item.setCart(cart);
        item.setSku("RICE-5KG");
        item.setProductName("Rice");
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setQuantity(2);
        item.setLineTotal(new BigDecimal("200.00"));
        when(orderRepository.findByIdempotencyKey("idem-auth")).thenReturn(Optional.empty());
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of(item));
        when(orderRepository.save(any(OrderEntity.class))).thenAnswer(invocation -> {
            OrderEntity entity = invocation.getArgument(0);
            setPrivateId(entity, 78L);
            return entity;
        });
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderSnapshot result = orderCheckoutService.placeOrder(
                "idem-auth",
                new PlaceOrderCommand(10L, "buyer@example.com"),
                9L,
                null
        );

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(orderCaptor.capture());
        assertEquals(9L, cart.getCustomerId());
        assertEquals(9L, orderCaptor.getValue().getCustomerId());
        assertEquals("buyer@example.com", result.customerEmail());
    }

    @Test
    void placeOrder_shouldNotReturnAnotherCustomersIdempotentOrder() throws Exception {
        OrderEntity existing = new OrderEntity();
        existing.setCustomerId(9L);
        when(orderRepository.findByIdempotencyKey("idem-other")).thenReturn(Optional.of(existing));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> orderCheckoutService.placeOrder(
                        "idem-other",
                        new PlaceOrderCommand(10L, null),
                        10L
                ));
    }

    @Test
    void placeOrder_shouldRejectMissingIdempotencyKey() {
        assertThrows(IllegalArgumentException.class,
                () -> orderCheckoutService.placeOrder("  ", new PlaceOrderCommand(10L, null)));
    }

    @Test
    void placeOrder_shouldRejectEmptyCart() throws Exception {
        CartEntity cart = activeCart(10L);
        when(orderRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(cartRepository.findById(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(10L)).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> orderCheckoutService.placeOrder(
                        "idem-1",
                        new PlaceOrderCommand(10L, null),
                        null,
                        GUEST_CART_TOKEN
                ));
    }

    private CartEntity activeCart(Long id) throws Exception {
        CartEntity cart = new CartEntity();
        setPrivateId(cart, id);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setCurrency("INR");
        cart.setSubtotalAmount(new BigDecimal("200.00"));
        cart.setTaxAmount(new BigDecimal("10.00"));
        cart.setDiscountAmount(new BigDecimal("20.00"));
        cart.setTotalAmount(new BigDecimal("190.00"));
        return cart;
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
