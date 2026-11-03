package com.riceoil.platform.orders.api.aftercare;

import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.orders.application.aftercare.CancellationSnapshot;
import com.riceoil.platform.orders.application.aftercare.RefundSnapshot;
import jakarta.validation.Valid;
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
                                                 @RequestBody(required = false) CancellationRequestHttp request) {
        CancellationSnapshot snapshot = aftercareService.requestCancellation(
                orderId,
                request == null ? null : request.reason()
        );
        return new CancellationResponseHttp(snapshot.id(), snapshot.orderId(), snapshot.status(), snapshot.reason());
    }

    @PostMapping("/{orderId}/refund-requests")
    public RefundResponseHttp refund(@PathVariable Long orderId,
                                     @Valid @RequestBody RefundRequestHttp request) {
        RefundSnapshot snapshot = aftercareService.requestRefund(orderId, request.amount(), request.reason());
        return new RefundResponseHttp(snapshot.id(), snapshot.orderId(), snapshot.amount(), snapshot.status(), snapshot.reason());
    }

    @GetMapping("/{orderId}/refunds")
    public List<RefundResponseHttp> refunds(@PathVariable Long orderId) {
        return aftercareService.refunds(orderId).stream()
                .map(snapshot -> new RefundResponseHttp(
                        snapshot.id(),
                        snapshot.orderId(),
                        snapshot.amount(),
                        snapshot.status(),
                        snapshot.reason()
                ))
                .toList();
    }
}
