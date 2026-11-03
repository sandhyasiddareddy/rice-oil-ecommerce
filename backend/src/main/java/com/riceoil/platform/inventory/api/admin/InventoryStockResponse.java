package com.riceoil.platform.inventory.api.admin;

public record InventoryStockResponse(
        Long variantId,
        int availableQty,
        int reservedQty
) {
}
