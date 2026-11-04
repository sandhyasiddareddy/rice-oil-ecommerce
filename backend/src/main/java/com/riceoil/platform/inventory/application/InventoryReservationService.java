package com.riceoil.platform.inventory.application;

import com.riceoil.platform.inventory.domain.InventoryReservationEntity;
import com.riceoil.platform.inventory.domain.InventoryReservationRepository;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import com.riceoil.platform.inventory.domain.InventoryStockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;

@Service
public class InventoryReservationService {

    public static final String ACTIVE = "ACTIVE";
    public static final String CONSUMED = "CONSUMED";
    public static final String RELEASED = "RELEASED";

    private final InventoryReservationRepository reservationRepository;
    private final InventoryStockRepository stockRepository;

    public InventoryReservationService(InventoryReservationRepository reservationRepository,
                                       InventoryStockRepository stockRepository) {
        this.reservationRepository = reservationRepository;
        this.stockRepository = stockRepository;
    }

    /**
     * Moves quantity from available to reserved. Variants without a stock record are untracked and skipped.
     */
    @Transactional
    public void reserve(Long variantId, int quantity, String reservationKey, OffsetDateTime expiresAt, String label) {
        Optional<InventoryStockEntity> found = stockRepository.findByVariantId(variantId);
        if (found.isEmpty()) {
            return;
        }
        InventoryStockEntity stock = found.get();
        if (stock.getAvailableQty() < quantity) {
            throw new IllegalArgumentException("Insufficient stock for " + label
                    + ": only " + stock.getAvailableQty() + " available");
        }
        stock.setAvailableQty(stock.getAvailableQty() - quantity);
        stock.setReservedQty(stock.getReservedQty() + quantity);
        stockRepository.save(stock);

        InventoryReservationEntity reservation = new InventoryReservationEntity();
        reservation.setVariantId(variantId);
        reservation.setReservationKey(reservationKey);
        reservation.setQuantity(quantity);
        reservation.setStatus(ACTIVE);
        reservation.setExpiresAt(expiresAt);
        reservationRepository.save(reservation);
    }

    /** Marks reservations as sold; the reserved quantity leaves stock for good. */
    @Transactional
    public void consume(String keyPrefix) {
        for (InventoryReservationEntity reservation
                : reservationRepository.findByReservationKeyStartingWithAndStatus(keyPrefix, ACTIVE)) {
            reservation.setStatus(CONSUMED);
            reservationRepository.save(reservation);
            stockRepository.findByVariantId(reservation.getVariantId()).ifPresent(stock -> {
                stock.setReservedQty(Math.max(0, stock.getReservedQty() - reservation.getQuantity()));
                stockRepository.save(stock);
            });
        }
    }

    /** Returns still-active reserved quantity to available stock. */
    @Transactional
    public void release(String keyPrefix) {
        for (InventoryReservationEntity reservation
                : reservationRepository.findByReservationKeyStartingWithAndStatus(keyPrefix, ACTIVE)) {
            reservation.setStatus(RELEASED);
            reservationRepository.save(reservation);
            stockRepository.findByVariantId(reservation.getVariantId()).ifPresent(stock -> {
                int freed = Math.min(reservation.getQuantity(), stock.getReservedQty());
                stock.setReservedQty(stock.getReservedQty() - freed);
                stock.setAvailableQty(stock.getAvailableQty() + freed);
                stockRepository.save(stock);
            });
        }
    }
}
