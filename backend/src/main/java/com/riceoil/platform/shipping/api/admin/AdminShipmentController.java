package com.riceoil.platform.shipping.api.admin;

import com.riceoil.platform.shipping.application.ShipmentSnapshot;
import com.riceoil.platform.shipping.application.ShipmentTrackingService;
import com.riceoil.platform.shipping.domain.ShipmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/admin/shipments")
public class AdminShipmentController {

    private final ShipmentTrackingService shipmentTrackingService;

    public AdminShipmentController(ShipmentTrackingService shipmentTrackingService) {
        this.shipmentTrackingService = shipmentTrackingService;
    }

    @PostMapping
    public ShipmentSnapshot create(@Valid @RequestBody CreateShipmentRequest request) {
        return shipmentTrackingService.createShipment(
                request.orderId(),
                request.shipmentCode(),
                request.provider(),
                request.trackingNumber(),
                request.trackingUrl()
        );
    }

    @PatchMapping("/{shipmentId}/status")
    public ShipmentSnapshot updateStatus(@PathVariable Long shipmentId,
                                         @Valid @RequestBody UpdateShipmentStatusRequest request) {
        return shipmentTrackingService.updateStatus(
                shipmentId,
                request.status(),
                request.description(),
                request.occurredAt()
        );
    }

    public record CreateShipmentRequest(
            @NotNull Long orderId,
            @NotBlank String shipmentCode,
            @NotBlank String provider,
            String trackingNumber,
            String trackingUrl
    ) {
    }

    public record UpdateShipmentStatusRequest(
            @NotNull ShipmentStatus status,
            String description,
            OffsetDateTime occurredAt
    ) {
    }
}
