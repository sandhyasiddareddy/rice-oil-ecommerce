package com.riceoil.platform.cart.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartItemEntity;
import com.riceoil.platform.cart.domain.CartItemRepository;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.domain.CartStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartApplicationServiceTest {

    private static final String GUEST_CART_TOKEN = "guest-cart-test-token";

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CheckoutQuoteService checkoutQuoteService;

    private CartApplicationService cartApplicationService;

    @BeforeEach
    void setUp() {
        this.cartApplicationService = new CartApplicationService(
                cartRepository,
                cartItemRepository,
                checkoutQuoteService,
                new ObjectMapper()
        );
        org.mockito.Mockito.lenient().when(checkoutQuoteService.quote(any(CheckoutQuoteRequest.class)))
                .thenAnswer(invocation -> {
                    CheckoutQuoteRequest request = invocation.getArgument(0);
                    BigDecimal subtotal = request.subtotal().setScale(2);
                    return new CheckoutQuoteResult(
                            subtotal,
                            new BigDecimal("0.00"),
                            subtotal,
                            request.taxCode(),
                            false,
                            null,
                            new BigDecimal("0.00"),
                            subtotal,
                            List.of()
                    );
                });
    }

    @Test
    void createCart_shouldDefaultToInr() throws Exception {
        ArgumentCaptor<CartEntity> cartCaptor = ArgumentCaptor.forClass(CartEntity.class);
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> {
            CartEntity cart = invocation.getArgument(0);
            setPrivateId(cart, 1L);
            return cart;
        });

        CartSnapshot snapshot = cartApplicationService.createCart(null);

        assertEquals(1L, snapshot.id());
        assertEquals("INR", snapshot.currency());
        assertEquals(new BigDecimal("0.00"), snapshot.totalAmount());
        verify(cartRepository).save(cartCaptor.capture());
        assertTrue(GuestCartAccessToken.matches(
                snapshot.guestAccessToken(),
                cartCaptor.getValue().getGuestAccessTokenHash()
        ));
    }

    @Test
    void addItem_shouldPersistLineTotal() throws Exception {
        CartEntity cart = new CartEntity();
        setPrivateId(cart, 5L);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency("INR");
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setSubtotalAmount(new BigDecimal("0.00"));
        cart.setTaxAmount(new BigDecimal("0.00"));
        cart.setDiscountAmount(new BigDecimal("0.00"));
        cart.setTotalAmount(new BigDecimal("0.00"));

        CartItemEntity savedItem = new CartItemEntity();
        setPrivateId(savedItem, 10L);
        savedItem.setCart(cart);
        savedItem.setSku("RICE-5KG");
        savedItem.setProductName("Rice");
        savedItem.setUnitPrice(new BigDecimal("100.00"));
        savedItem.setQuantity(2);
        savedItem.setLineTotal(new BigDecimal("200.00"));

        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenReturn(savedItem);
        when(cartItemRepository.findByCartIdOrderByIdAsc(5L)).thenReturn(List.of(), List.of(savedItem));

        CartSnapshot snapshot = cartApplicationService.addItem(
                5L,
                new AddCartItemCommand("rice-5kg", "Rice", new BigDecimal("100"), 2),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(1, snapshot.items().size());
        assertEquals(new BigDecimal("200.00"), snapshot.items().get(0).lineTotal());
        assertEquals(new BigDecimal("200.00"), snapshot.subtotalAmount());
        assertEquals(new BigDecimal("200.00"), snapshot.totalAmount());
    }

    @Test
    void addItem_shouldMergeQuantityWhenSkuAlreadyInCart() throws Exception {
        CartEntity cart = new CartEntity();
        setPrivateId(cart, 5L);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency("INR");
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setSubtotalAmount(new BigDecimal("0.00"));
        cart.setTaxAmount(new BigDecimal("0.00"));
        cart.setDiscountAmount(new BigDecimal("0.00"));
        cart.setTotalAmount(new BigDecimal("0.00"));

        CartItemEntity existing = new CartItemEntity();
        setPrivateId(existing, 10L);
        existing.setCart(cart);
        existing.setSku("RICE-5KG");
        existing.setProductName("Rice");
        existing.setUnitPrice(new BigDecimal("100.00"));
        existing.setQuantity(1);
        existing.setLineTotal(new BigDecimal("100.00"));

        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenReturn(existing);
        when(cartItemRepository.findByCartIdOrderByIdAsc(5L)).thenReturn(List.of(existing));

        CartSnapshot snapshot = cartApplicationService.addItem(
                5L,
                new AddCartItemCommand("rice-5kg", "Rice", new BigDecimal("100"), 1),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(1, snapshot.items().size());
        assertEquals(2, snapshot.items().get(0).quantity());
        assertEquals(new BigDecimal("200.00"), snapshot.items().get(0).lineTotal());
    }

    @Test
    void refreshPricing_shouldUpdateCartTotals() throws Exception {
        CartEntity cart = new CartEntity();
        setPrivateId(cart, 8L);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency("INR");
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setSubtotalAmount(new BigDecimal("0.00"));
        cart.setTaxAmount(new BigDecimal("0.00"));
        cart.setDiscountAmount(new BigDecimal("0.00"));
        cart.setTotalAmount(new BigDecimal("0.00"));

        CartItemEntity item = new CartItemEntity();
        setPrivateId(item, 17L);
        item.setCart(cart);
        item.setSku("OIL-1L");
        item.setProductName("Groundnut Oil");
        item.setUnitPrice(new BigDecimal("200.00"));
        item.setQuantity(1);
        item.setLineTotal(new BigDecimal("200.00"));

        when(cartRepository.findById(8L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(8L)).thenReturn(List.of(item));
        when(checkoutQuoteService.quote(any(CheckoutQuoteRequest.class))).thenReturn(
                new CheckoutQuoteResult(
                        new BigDecimal("200.00"),
                        new BigDecimal("10.00"),
                        new BigDecimal("210.00"),
                        "GST5",
                        false,
                        "SAVE20",
                        new BigDecimal("20.00"),
                        new BigDecimal("190.00"),
                        List.of()
                )
        );
        when(cartRepository.save(any(CartEntity.class))).thenReturn(cart);

        CartSnapshot snapshot = cartApplicationService.refreshPricing(
                8L,
                new CartPricingRefreshCommand("GST5", List.of("SAVE20")),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(new BigDecimal("200.00"), snapshot.subtotalAmount());
        assertEquals(new BigDecimal("10.00"), snapshot.taxAmount());
        assertEquals(new BigDecimal("20.00"), snapshot.discountAmount());
        assertEquals(new BigDecimal("190.00"), snapshot.totalAmount());

        ArgumentCaptor<CheckoutQuoteRequest> requestCaptor = ArgumentCaptor.forClass(CheckoutQuoteRequest.class);
        org.mockito.Mockito.verify(checkoutQuoteService).quote(requestCaptor.capture());
        assertEquals(new BigDecimal("200.00"), requestCaptor.getValue().subtotal());
        assertEquals("GST5", requestCaptor.getValue().taxCode());
        assertEquals(List.of("SAVE20"), requestCaptor.getValue().couponCodes());
    }

    @Test
    void addItem_shouldRejectInvalidQuantity() {
        assertThrows(IllegalArgumentException.class, () -> cartApplicationService.addItem(
                1L,
                new AddCartItemCommand("SKU", "Name", new BigDecimal("10"), 0)
        ));
    }

    @Test
    void updateItem_shouldRecalculateLineTotal() throws Exception {
        CartEntity cart = cart(5L);
        CartItemEntity item = item(10L, cart, new BigDecimal("100.00"), 2);

        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 5L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any(CartItemEntity.class))).thenReturn(item);
        when(cartItemRepository.findByCartIdOrderByIdAsc(5L)).thenReturn(List.of(item));

        CartSnapshot snapshot = cartApplicationService.updateItem(
                5L,
                10L,
                new UpdateCartItemCommand(new BigDecimal("120.00"), 3),
                null,
                GUEST_CART_TOKEN
        );

        assertEquals(new BigDecimal("360.00"), snapshot.items().get(0).lineTotal());
        assertEquals(new BigDecimal("360.00"), snapshot.subtotalAmount());
        assertEquals(new BigDecimal("360.00"), snapshot.totalAmount());
    }

    @Test
    void removeItem_shouldDeleteFromCart() throws Exception {
        CartEntity cart = cart(5L);
        CartItemEntity item = item(10L, cart, new BigDecimal("100.00"), 2);

        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(10L, 5L)).thenReturn(Optional.of(item));
        when(cartItemRepository.findByCartIdOrderByIdAsc(5L)).thenReturn(List.of());

        CartSnapshot snapshot = cartApplicationService.removeItem(5L, 10L, null, GUEST_CART_TOKEN);

        verify(cartItemRepository).delete(item);
        assertEquals(0, snapshot.items().size());
        assertEquals(new BigDecimal("0.00"), snapshot.totalAmount());
    }

    @Test
    void getCart_shouldRepairStaleSummaryAmounts() throws Exception {
        CartEntity cart = cart(5L);
        CartItemEntity item = item(10L, cart, new BigDecimal("119.00"), 1);
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByIdAsc(5L)).thenReturn(List.of(item));

        CartSnapshot snapshot = cartApplicationService.getCart(5L, null, GUEST_CART_TOKEN);

        assertEquals(new BigDecimal("119.00"), snapshot.subtotalAmount());
        assertEquals(new BigDecimal("119.00"), snapshot.totalAmount());
    }

    @Test
    void getOrCreateCustomerCart_shouldClaimGuestCart() throws Exception {
        CartEntity guestCart = cart(21L);
        when(cartRepository.findFirstByCustomerIdAndStatusOrderByUpdatedAtDesc(9L, CartStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(cartRepository.findById(21L)).thenReturn(Optional.of(guestCart));
        when(cartRepository.save(any(CartEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartItemRepository.findByCartIdOrderByIdAsc(21L)).thenReturn(List.of());

        CartSnapshot snapshot = cartApplicationService.getOrCreateCustomerCart(9L, 21L, GUEST_CART_TOKEN);

        assertEquals(21L, snapshot.id());
        assertEquals(9L, guestCart.getCustomerId());
    }

    @Test
    void getCart_shouldRejectAnotherCustomersCart() throws Exception {
        CartEntity cart = cart(21L);
        cart.setCustomerId(99L);
        when(cartRepository.findById(21L)).thenReturn(Optional.of(cart));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> cartApplicationService.getCart(21L, 9L));
    }

    private CartEntity cart(Long id) throws Exception {
        CartEntity cart = new CartEntity();
        setPrivateId(cart, id);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setCurrency("INR");
        cart.setGuestAccessTokenHash(GuestCartAccessToken.hash(GUEST_CART_TOKEN));
        cart.setSubtotalAmount(new BigDecimal("0.00"));
        cart.setTaxAmount(new BigDecimal("0.00"));
        cart.setDiscountAmount(new BigDecimal("0.00"));
        cart.setTotalAmount(new BigDecimal("0.00"));
        return cart;
    }

    private CartItemEntity item(Long id, CartEntity cart, BigDecimal unitPrice, int quantity) throws Exception {
        CartItemEntity item = new CartItemEntity();
        setPrivateId(item, id);
        item.setCart(cart);
        item.setSku("SKU");
        item.setProductName("Name");
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setLineTotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
