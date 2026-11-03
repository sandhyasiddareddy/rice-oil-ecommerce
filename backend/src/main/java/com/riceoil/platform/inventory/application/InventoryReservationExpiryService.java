package com.riceoil.platform.inventory.application;

import com.riceoil.platform.inventory.domain.InventoryReservationEntity;
import com.riceoil.platform.inventory.domain.InventoryReservationRepository;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import com.riceoil.platform.inventory.domain.InventoryStockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class InventoryReservationExpiryService {

    private static final String ACTIVE = "ACTIVE";
    private static final String EXPIRED = "EXPIRED";

    private final InventoryReservationRepository inventoryReservationRepository;
    private final InventoryStockRepository inventoryStockRepository;

    public InventoryReservationExpiryService(InventoryReservationRepository inventoryReservationRepository,
                                            InventoryStockRepository inventoryStockRepository) {
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.inventoryStockRepository = inventoryStockRepository;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public int expireReservations() {
        List<InventoryReservationEntity> reservations = inventoryReservationRepository
                .findByStatusAndExpiresAtBefore(ACTIVE, OffsetDateTime.now());

        for (InventoryReservationEntity reservation : reservations) {
            reservation.setStatus(EXPIRED);
            inventoryReservationRepository.save(reservation);

            inventoryStockRepository.findByVariantId(reservation.getVariantId())
                    .ifPresent(stock -> {
                        int freedQty = reservation.getQuantity();
                        if (stock.getReservedQty() >= freedQty) {
                            stock.setReservedQty(stock.getReservedQty() - freedQty);
                            stock.setAvailableQty(stock.getAvailableQty() + freedQty);
                            inventoryStockRepository.save(stock);
                        }
                    });
        }

        return reservations.size();
    }
}
