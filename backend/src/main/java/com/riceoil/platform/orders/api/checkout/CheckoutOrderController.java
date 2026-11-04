package com.riceoil.platform.orders.api.checkout;

import com.riceoil.platform.orders.application.OrderCheckoutService;
import com.riceoil.platform.orders.application.OrderSnapshot;
import com.riceoil.platform.orders.application.PlaceOrderCommand;
import com.riceoil.platform.orders.application.GuestOrderAccessService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutOrderController {

    private final OrderCheckoutService orderCheckoutService;
    private final GuestOrderAccessService guestOrderAccessService;

    public CheckoutOrderController(OrderCheckoutService orderCheckoutService,
                                   GuestOrderAccessService guestOrderAccessService) {
        this.orderCheckoutService = orderCheckoutService;
        this.guestOrderAccessService = guestOrderAccessService;
    }

    @PostMapping("/orders")
    public CheckoutOrderHttpResponse placeOrder(
            @RequestHeader(name = "Idempotency-Key") String idempotencyKey,
            @RequestHeader(name = "X-Guest-Cart-Token", required = false) String guestCartToken,
            @Valid @RequestBody CheckoutOrderHttpRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        String customerEmail = jwt == null ? request.customerEmail() : jwt.getSubject();
        PlaceOrderCommand command = new PlaceOrderCommand(
                request.cartId(),
                customerEmail,
                request.shippingAddress().toSnapshot(),
                request.billingAddress() == null ? null : request.billingAddress().toSnapshot()
        );
        OrderSnapshot order = orderCheckoutService.placeOrder(
                idempotencyKey,
                command,
                jwt == null ? null : customerId(jwt),
                guestCartToken
        );

        return new CheckoutOrderHttpResponse(
                order.id(),
                order.orderNumber(),
                order.cartId(),
                order.status(),
                order.currency(),
                order.subtotalAmount(),
                order.taxAmount(),
                order.discountAmount(),
                order.totalAmount(),
                order.customerEmail(),
                jwt == null ? guestOrderAccessService.issueToken(order.id()).accessToken() : null
        );
    }

    private Long customerId(Jwt jwt) {
        Object customerId = jwt.getClaim("customerId");
        if (customerId instanceof Number number) {
            return number.longValue();
        }
        throw new IllegalStateException("Authenticated token is missing customer identity");
    }
}
