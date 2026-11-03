package com.riceoil.platform.shipping.application;

import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.application.CartApplicationService;
import com.riceoil.platform.shipping.domain.ShippingQuoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class ShippingQuoteServiceTest {

    @Test
    void createQuote_shouldRejectCartOwnedByAnotherCustomer() throws Exception {
        CartRepository cartRepository = mock(CartRepository.class);
        CartApplicationService cartApplicationService = mock(CartApplicationService.class);
        ShippingQuoteRepository quoteRepository = mock(ShippingQuoteRepository.class);
        CartEntity cart = new CartEntity();
        cart.setCustomerId(42L);
        var idField = CartEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(cart, 5L);
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        ShippingQuoteService service = new ShippingQuoteService(
                cartRepository,
                cartApplicationService,
                quoteRepository
        );

        org.mockito.Mockito.doThrow(new AccessDeniedException("Denied"))
                .when(cartApplicationService).verifyCartAccess(cart, 99L, null);
        assertThrows(AccessDeniedException.class, () -> service.createQuote(5L, 99L, null));
        verify(quoteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createQuote_shouldValidateGuestCartAccessToken() throws Exception {
        CartRepository cartRepository = mock(CartRepository.class);
        CartApplicationService cartApplicationService = mock(CartApplicationService.class);
        ShippingQuoteRepository quoteRepository = mock(ShippingQuoteRepository.class);
        CartEntity cart = new CartEntity();
        cart.setCurrency("INR");
        cart.setTotalAmount(java.math.BigDecimal.ZERO);
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));
        ShippingQuoteService service = new ShippingQuoteService(
                cartRepository,
                cartApplicationService,
                quoteRepository
        );

        org.mockito.Mockito.doThrow(new AccessDeniedException("Denied"))
                .when(cartApplicationService).verifyCartAccess(cart, null, "invalid-token");

        assertThrows(AccessDeniedException.class, () -> service.createQuote(5L, null, "invalid-token"));
        verify(cartApplicationService).verifyCartAccess(cart, null, "invalid-token");
        verify(quoteRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
