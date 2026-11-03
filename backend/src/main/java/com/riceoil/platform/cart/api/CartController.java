package com.riceoil.platform.cart.api;

import com.riceoil.platform.cart.application.AddCartItemCommand;
import com.riceoil.platform.cart.application.CartApplicationService;
import com.riceoil.platform.cart.application.CartItemSnapshot;
import com.riceoil.platform.cart.application.CartPricingRefreshCommand;
import com.riceoil.platform.cart.application.CartSnapshot;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartApplicationService cartApplicationService;

    public CartController(CartApplicationService cartApplicationService) {
        this.cartApplicationService = cartApplicationService;
    }

    @PostMapping
    public CartResponse createCart(@RequestBody(required = false) CreateCartRequest request,
                                   @AuthenticationPrincipal Jwt jwt) {
        String currency = request == null ? null : request.currency();
        return toResponse(jwt == null
                ? cartApplicationService.createCart(currency)
                : cartApplicationService.getOrCreateCustomerCart(customerId(jwt), null));
    }

    @GetMapping("/{cartId}")
    public CartResponse getCart(@PathVariable Long cartId,
                                @AuthenticationPrincipal Jwt jwt,
                                @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestToken) {
        return toResponse(cartApplicationService.getCart(cartId, customerId(jwt), guestToken));
    }

    @PostMapping("/{cartId}/items")
    public CartResponse addItem(@PathVariable Long cartId,
                                @Valid @RequestBody AddCartItemRequest request,
                                @AuthenticationPrincipal Jwt jwt,
                                @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestToken) {
        AddCartItemCommand command = new AddCartItemCommand(
                request.sku(),
                request.productName(),
                request.unitPrice(),
                request.quantity()
        );
        return toResponse(cartApplicationService.addItem(cartId, command, customerId(jwt), guestToken));
    }

    @PatchMapping("/{cartId}/items/{itemId}")
    public CartResponse updateItem(@PathVariable Long cartId,
                                   @PathVariable Long itemId,
                                   @RequestBody(required = false) UpdateCartItemRequest request,
                                   @AuthenticationPrincipal Jwt jwt,
                                   @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestToken) {
        UpdateCartItemRequest safeRequest = request == null ? new UpdateCartItemRequest(null, null) : request;
        var command = new com.riceoil.platform.cart.application.UpdateCartItemCommand(
                safeRequest.unitPrice(),
                safeRequest.quantity()
        );
        return toResponse(cartApplicationService.updateItem(cartId, itemId, command, customerId(jwt), guestToken));
    }

    @DeleteMapping("/{cartId}/items/{itemId}")
    public CartResponse removeItem(@PathVariable Long cartId,
                                   @PathVariable Long itemId,
                                   @AuthenticationPrincipal Jwt jwt,
                                   @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestToken) {
        return toResponse(cartApplicationService.removeItem(cartId, itemId, customerId(jwt), guestToken));
    }

    @PostMapping("/{cartId}/pricing/refresh")
    public CartResponse refreshPricing(@PathVariable Long cartId,
                                       @RequestBody(required = false) CartPricingRefreshRequest request,
                                       @AuthenticationPrincipal Jwt jwt,
                                       @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestToken) {
        CartPricingRefreshRequest safeRequest = request == null
                ? new CartPricingRefreshRequest(null, List.of())
                : request;

        CartPricingRefreshCommand command = new CartPricingRefreshCommand(safeRequest.taxCode(), safeRequest.couponCodes());
        return toResponse(cartApplicationService.refreshPricing(cartId, command, customerId(jwt), guestToken));
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
}
