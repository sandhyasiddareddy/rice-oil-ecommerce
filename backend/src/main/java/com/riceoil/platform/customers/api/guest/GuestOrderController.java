package com.riceoil.platform.customers.api.guest;

import com.riceoil.platform.orders.application.GuestOrderAccessService;
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

    public GuestOrderController(GuestOrderAccessService guestOrderAccessService,
                                ShipmentTrackingService shipmentTrackingService) {
        this.guestOrderAccessService = guestOrderAccessService;
        this.shipmentTrackingService = shipmentTrackingService;
    }

    @GetMapping("/{orderId}")
    public GuestOrderStatusResponse orderStatus(@PathVariable Long orderId,
                                                @RequestHeader("X-Guest-Token") String token) {
        var order = guestOrderAccessService.verifyAndGetOrder(orderId, token);
        return new GuestOrderStatusResponse(
                order.orderId(),
                order.orderNumber(),
                order.status().name(),
                List.of()
        );
    }

    @GetMapping("/{orderId}/tracking")
    public GuestOrderStatusResponse trackingStatus(@PathVariable Long orderId,
                                                   @RequestHeader("X-Guest-Token") String token) {
        var order = guestOrderAccessService.verifyAndGetOrder(orderId, token);
        List<ShipmentSnapshot> shipments = shipmentTrackingService.getOrderShipments(orderId);
        return new GuestOrderStatusResponse(
                order.orderId(),
                order.orderNumber(),
                order.status().name(),
                shipments
        );
    }
}
