package com.riceoil.platform.inventory.api.admin;

import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryCommandService inventoryCommandService;

    public AdminInventoryController(InventoryCommandService inventoryCommandService) {
        this.inventoryCommandService = inventoryCommandService;
    }

    @PostMapping("/adjustments")
    public InventoryStockResponse adjustInventory(@Valid @RequestBody AdjustInventoryRequest request) {
        InventoryStockEntity stock = inventoryCommandService.adjustStock(
                request.variantId(),
                request.deltaQty(),
                request.reason(),
                request.reference(),
                request.createdBy()
        );

        return new InventoryStockResponse(stock.getVariantId(), stock.getAvailableQty(), stock.getReservedQty());
    }
}
