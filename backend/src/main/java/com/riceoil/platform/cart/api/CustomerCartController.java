package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.application.CartApplicationService;
import com.riceoil.platform.cart.application.CartItemSnapshot;
import com.riceoil.platform.cart.application.CartSnapshot;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/me/cart")
public class CustomerCartController {

    private final CartApplicationService cartApplicationService;

    public CustomerCartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    @PostMapping
    public CartResponse getOrCreate(@AuthenticationPrincipal Jwt jwt,
                                    @RequestBody(required = false) GuestCartRequest request,
                                    @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestAccessToken) {
        Long guestCartId = request == null ? null : request.guestCartId();
        Object customerIdClaim = jwt.getClaim("customerId");
        if (!(customerIdClaim instanceof Number customerId)) {
            throw new IllegalStateException("Authenticated token is missing customer identity");
        }
        return toResponse(cartApplicationService.getOrCreateCustomerCart(customerId.longValue(), guestCartId, guestAccessToken));
    }

    private CartResponse toResponse(CartSnapshot snapshot) {
        List<CartItemResponse> items = snapshot.items().stream()
                .map(this::toItemResponse)
                .toList();
        return new CartResponse(
                snapshot.id(),
                snapshot.status(),
                snapshot.currency(),
                snapshot.taxCode(),
                snapshot.subtotalAmount(),
                snapshot.taxAmount(),
                snapshot.discountAmount(),
                snapshot.totalAmount(),
                snapshot.couponCodes(),
                items,
                snapshot.guestAccessToken()
        );
    }

    private CartItemResponse toItemResponse(CartItemSnapshot item) {
        return new CartItemResponse(
                item.id(),
                item.sku(),
                item.productName(),
                item.unitPrice(),
                item.quantity(),
                item.lineTotal()
        );
    }

    public record GuestCartRequest(Long guestCartId) {
    }
}
