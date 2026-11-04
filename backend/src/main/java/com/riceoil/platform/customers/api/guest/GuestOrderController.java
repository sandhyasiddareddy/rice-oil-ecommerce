package com.riceoil.platform.customers.api.guest;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
import com.riceoil.platform.orders.application.OrderItemsQueryService;
import com.riceoil.platform.orders.application.aftercare.AftercareService;
import com.riceoil.platform.shipping.application.ShipmentSnapshot;
import com.riceoil.platform.shipping.application.ShipmentTrackingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/guest/orders")
public class GuestOrderController {

    private final GuestOrderAccessService guestOrderAccessService;
    private final ShipmentTrackingService shipmentTrackingService;
    private final AftercareService aftercareService;
    private final OrderItemsQueryService orderItemsQueryService;

    public GuestOrderController(GuestOrderAccessService guestOrderAccessService,
                                ShipmentTrackingService shipmentTrackingService,
                                AftercareService aftercareService,
                                OrderItemsQueryService orderItemsQueryService) {
        this.guestOrderAccessService = guestOrderAccessService;
        this.shipmentTrackingService = shipmentTrackingService;
        this.aftercareService = aftercareService;
        this.orderItemsQueryService = orderItemsQueryService;
    }

    @GetMapping("/{orderId}")
    public GuestOrderStatusResponse orderStatus(@PathVariable Long orderId,
                                                @RequestHeader("X-Guest-Token") String token) {
        var order = guestOrderAccessService.verifyAndGetOrder(orderId, token);
        var aftercare = aftercareService.summary(orderId);
        var lines = orderItemsQueryService.linesFor(orderId);
        return new GuestOrderStatusResponse(
                order.orderId(),
                order.orderNumber(),
                order.status().name(),
                order.currency(),
                order.totalAmount(),
                lines.shippingAmount(),
                lines.items(),
                List.of(),
                aftercare.canRequestCancellation(),
                aftercare.cancellationStatus(),
                aftercare.refundStatus(),
                aftercare.refundAmount()
        );
    }

    @GetMapping("/{orderId}/tracking")
    public GuestOrderStatusResponse trackingStatus(@PathVariable Long orderId,
                                                   @RequestHeader("X-Guest-Token") String token) {
        var order = guestOrderAccessService.verifyAndGetOrder(orderId, token);
        List<ShipmentSnapshot> shipments = shipmentTrackingService.getOrderShipments(orderId);
        var aftercare = aftercareService.summary(orderId);
        var lines = orderItemsQueryService.linesFor(orderId);
        return new GuestOrderStatusResponse(
                order.orderId(),
                order.orderNumber(),
                order.status().name(),
                order.currency(),
                order.totalAmount(),
                lines.shippingAmount(),
                lines.items(),
                shipments,
                aftercare.canRequestCancellation(),
                aftercare.cancellationStatus(),
                aftercare.refundStatus(),
                aftercare.refundAmount()
        );
    }
}
