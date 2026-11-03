package com.riceoil.platform.shipping.application;

import com.riceoil.platform.cart.domain.CartEntity;
import com.riceoil.platform.cart.domain.CartRepository;
import com.riceoil.platform.cart.application.CartApplicationService;
import com.riceoil.platform.shipping.domain.ShippingQuoteEntity;
import com.riceoil.platform.shipping.domain.ShippingQuoteRepository;
import com.riceoil.platform.shipping.domain.ShippingQuoteStatus;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ShippingQuoteService {

    private final CartRepository cartRepository;
    private final CartApplicationService cartApplicationService;
    private final ShippingQuoteRepository shippingQuoteRepository;

    public ShippingQuoteService(CartRepository cartRepository,
                                CartApplicationService cartApplicationService,
                                ShippingQuoteRepository shippingQuoteRepository) {
        this.cartRepository = cartRepository;
        this.cartApplicationService = cartApplicationService;
        this.shippingQuoteRepository = shippingQuoteRepository;
    }

    @Transactional
    public ShippingQuoteSnapshot createQuote(Long cartId, Long customerId, String guestCartToken) {
        CartEntity cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found: " + cartId));
        cartApplicationService.verifyCartAccess(cart, customerId, guestCartToken);

        ShippingQuoteEntity quote = new ShippingQuoteEntity();
        quote.setQuoteCode(generateQuoteCode());
        quote.setCart(cart);
        quote.setStatus(ShippingQuoteStatus.AVAILABLE);
        quote.setShippingFee(computeShippingFee(cart.getTotalAmount()));
        quote.setCurrency(cart.getCurrency());
        quote.setProvider("SHIPROCKET");
        quote.setServiceLevel("STANDARD");
        quote.setExpiresAt(OffsetDateTime.now().plusHours(2));

        ShippingQuoteEntity saved = shippingQuoteRepository.save(quote);
        return toSnapshot(saved);
    }

    @Transactional(readOnly = true)
    public ShippingQuoteSnapshot getQuote(String quoteCode, Long customerId, String guestCartToken) {
        ShippingQuoteEntity quote = shippingQuoteRepository.findByQuoteCode(quoteCode)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping quote not found: " + quoteCode));
        cartApplicationService.verifyCartAccess(quote.getCart(), customerId, guestCartToken);

        ShippingQuoteStatus status = quote.getExpiresAt().isBefore(OffsetDateTime.now())
                ? ShippingQuoteStatus.EXPIRED
                : quote.getStatus();
        quote.setStatus(status);
        return toSnapshot(quote);
    }

    private BigDecimal computeShippingFee(BigDecimal orderTotal) {
        if (orderTotal == null) {
            return new BigDecimal("80.00");
        }
        if (orderTotal.compareTo(new BigDecimal("999.00")) >= 0) {
            return new BigDecimal("0.00");
        }
        if (orderTotal.compareTo(new BigDecimal("499.00")) >= 0) {
            return new BigDecimal("40.00");
        }
        return new BigDecimal("80.00");
    }

    private String generateQuoteCode() {
        String tail = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        return "SQ-" + tail;
    }

    private ShippingQuoteSnapshot toSnapshot(ShippingQuoteEntity quote) {
        return new ShippingQuoteSnapshot(
                quote.getQuoteCode(),
                quote.getCart().getId(),
                quote.getStatus(),
                quote.getShippingFee(),
                quote.getCurrency(),
                quote.getProvider(),
                quote.getServiceLevel(),
                quote.getExpiresAt()
        );
    }
}
