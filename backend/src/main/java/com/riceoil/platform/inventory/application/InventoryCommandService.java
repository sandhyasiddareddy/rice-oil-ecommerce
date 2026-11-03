package com.riceoil.platform.inventory.application;

import com.riceoil.platform.inventory.domain.InventoryAdjustmentEntity;
import com.riceoil.platform.inventory.domain.InventoryAdjustmentRepository;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import com.riceoil.platform.inventory.domain.InventoryStockRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryCommandService {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryAdjustmentRepository inventoryAdjustmentRepository;

    public InventoryCommandService(InventoryStockRepository inventoryStockRepository,
                                   InventoryAdjustmentRepository inventoryAdjustmentRepository) {
        this.inventoryStockRepository = inventoryStockRepository;
        this.inventoryAdjustmentRepository = inventoryAdjustmentRepository;
    }

    @Transactional
    public void ensureStockRecord(Long variantId) {
        inventoryStockRepository.findByVariantId(variantId)
                .orElseGet(() -> {
                    InventoryStockEntity stock = new InventoryStockEntity();
                    stock.setVariantId(variantId);
                    stock.setAvailableQty(0);
                    stock.setReservedQty(0);
                    return inventoryStockRepository.save(stock);
                });
    }

    @Transactional
    public InventoryStockEntity adjustStock(Long variantId,
                                            int deltaQty,
                                            String reason,
                                            String reference,
                                            String createdBy) {
        if (deltaQty == 0) {
            throw new IllegalArgumentException("deltaQty must not be zero");
        }

        InventoryStockEntity stock = inventoryStockRepository.findByVariantId(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Stock record not found for variant: " + variantId));

        int nextQty = stock.getAvailableQty() + deltaQty;
        if (nextQty < 0) {
            throw new IllegalArgumentException("Stock cannot become negative");
        }

        stock.setAvailableQty(nextQty);

        InventoryAdjustmentEntity adjustment = new InventoryAdjustmentEntity();
        adjustment.setVariantId(variantId);
        adjustment.setDeltaQty(deltaQty);
        adjustment.setReason(reason);
        adjustment.setReference(reference);
        adjustment.setCreatedBy(createdBy);

        inventoryAdjustmentRepository.save(adjustment);
        return inventoryStockRepository.save(stock);
    }
}
