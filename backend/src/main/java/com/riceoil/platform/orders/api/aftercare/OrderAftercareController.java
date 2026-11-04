package com.riceoil.platform.orders.api.aftercare;

import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.orders.application.aftercare.CancellationSnapshot;
import com.riceoil.platform.orders.application.aftercare.RefundSnapshot;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderAftercareController {

    private final AftercareService aftercareService;

    public OrderAftercareController(AftercareService aftercareService) {
        this.aftercareService = aftercareService;
    }

    @PostMapping("/{orderId}/cancellation-requests")
    public CancellationResponseHttp cancellation(@PathVariable Long orderId,
                                                 @RequestBody(required = false) CancellationRequestHttp request,
                                                 @AuthenticationPrincipal Jwt jwt,
                                                 @org.springframework.web.bind.annotation.RequestHeader(
                                                         name = "X-Guest-Order-Token", required = false)
                                                 String guestOrderToken) {
        CancellationSnapshot snapshot = aftercareService.requestCancellation(
                orderId,
                request == null ? null : request.reason(),
                customerId(jwt),
                guestOrderToken
        );
        return new CancellationResponseHttp(snapshot.id(), snapshot.orderId(), snapshot.status(), snapshot.reason());
    }

    @PostMapping("/{orderId}/refund-requests")
    public RefundResponseHttp refund(@PathVariable Long orderId,
                                     @Valid @RequestBody RefundRequestHttp request,
                                     @AuthenticationPrincipal Jwt jwt,
                                     @org.springframework.web.bind.annotation.RequestHeader(
                                             name = "X-Guest-Order-Token", required = false)
                                     String guestOrderToken) {
        RefundSnapshot snapshot = aftercareService.requestRefund(
                orderId, request.amount(), request.reason(), customerId(jwt), guestOrderToken);
        return toResponse(snapshot);
    }

    @GetMapping("/{orderId}/refunds")
    public List<RefundResponseHttp> refunds(@PathVariable Long orderId,
                                            @AuthenticationPrincipal Jwt jwt,
                                            @org.springframework.web.bind.annotation.RequestHeader(
                                                    name = "X-Guest-Order-Token", required = false)
                                            String guestOrderToken) {
        return aftercareService.refunds(orderId, customerId(jwt), guestOrderToken).stream()
                .map(this::toResponse)
                .toList();
    }

    private RefundResponseHttp toResponse(RefundSnapshot snapshot) {
        return new RefundResponseHttp(snapshot.id(), snapshot.orderId(), snapshot.amount(),
                snapshot.status(), snapshot.reason(), snapshot.providerRefundId(), snapshot.providerStatus());
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
}
