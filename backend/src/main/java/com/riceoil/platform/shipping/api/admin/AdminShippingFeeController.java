package com.riceoil.platform.shipping.api.admin;

import com.riceoil.platform.shipping.application.ShippingFeeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/shipping/fee-tiers")
public class AdminShippingFeeController {

    private final ShippingFeeService shippingFeeService;

    public AdminShippingFeeController(ShippingFeeService shippingFeeService) {
        this.shippingFeeService = shippingFeeService;
    }

    @GetMapping
    public List<ShippingFeeService.Tier> list() {
        return shippingFeeService.listTiers();
    }

    @PutMapping
    public List<ShippingFeeService.Tier> replace(@Valid @RequestBody ReplaceRequest request) {
        return shippingFeeService.replaceTiers(request.tiers());
    }

    public record ReplaceRequest(@NotNull List<ShippingFeeService.Tier> tiers) {
    }
}
