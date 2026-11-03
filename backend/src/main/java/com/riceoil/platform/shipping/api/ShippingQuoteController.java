package com.riceoil.platform.shipping.api;

import com.riceoil.platform.shipping.application.ShippingQuoteService;
import com.riceoil.platform.shipping.application.ShippingQuoteSnapshot;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping/quotes")
public class ShippingQuoteController {

    private final ShippingQuoteService shippingQuoteService;

    public ShippingQuoteController(ShippingQuoteService shippingQuoteService) {
        this.shippingQuoteService = shippingQuoteService;
    }

    @PostMapping
    public ShippingQuoteResponse create(@Valid @RequestBody ShippingQuoteRequest request,
                                        @AuthenticationPrincipal Jwt jwt,
                                        @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestCartToken) {
        return toResponse(shippingQuoteService.createQuote(request.cartId(), customerId(jwt), guestCartToken));
    }

    @GetMapping("/{quoteCode}")
    public ShippingQuoteResponse get(@PathVariable String quoteCode,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestCartToken) {
        return toResponse(shippingQuoteService.getQuote(quoteCode, customerId(jwt), guestCartToken));
    }

    private Long customerId(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        Object customerId = jwt.getClaim("customerId");
        if (customerId instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Authenticated token is missing customer identity");
    }

    private ShippingQuoteResponse toResponse(ShippingQuoteSnapshot snapshot) {
        return new ShippingQuoteResponse(
                snapshot.quoteCode(),
                snapshot.cartId(),
                snapshot.status(),
                snapshot.shippingFee(),
                snapshot.currency(),
                snapshot.provider(),
                snapshot.serviceLevel(),
                snapshot.expiresAt()
        );
    }
}
