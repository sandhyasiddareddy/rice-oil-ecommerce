package com.riceoil.platform.inventory.application;

import com.riceoil.platform.inventory.domain.InventoryReservationEntity;
import com.riceoil.platform.inventory.domain.InventoryReservationRepository;
import com.riceoil.platform.inventory.domain.InventoryStockEntity;
import com.riceoil.platform.inventory.domain.InventoryStockRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class InventoryReservationServiceTest {

    @Mock
    private InventoryReservationRepository reservationRepository;

    @Mock
    private InventoryStockRepository stockRepository;

    private InventoryReservationService service() {
        return new InventoryReservationService(reservationRepository, stockRepository);
    }

    private InventoryStockEntity stock(int available, int reserved) {
        InventoryStockEntity stock = new InventoryStockEntity();
        stock.setVariantId(1L);
        stock.setAvailableQty(available);
        stock.setReservedQty(reserved);
        return stock;
    }

    private InventoryReservationEntity activeReservation(int qty) {
        InventoryReservationEntity reservation = new InventoryReservationEntity();
        reservation.setVariantId(1L);
        reservation.setQuantity(qty);
        reservation.setStatus("ACTIVE");
        return reservation;
    }

    @Test
    void reserve_movesAvailableToReserved() {
        InventoryStockEntity stock = stock(10, 0);
        when(stockRepository.findByVariantId(1L)).thenReturn(Optional.of(stock));

        service().reserve(1L, 3, "ORD-1:1", OffsetDateTime.now().plusHours(1), "Rice");

        assertEquals(7, stock.getAvailableQty());
        assertEquals(3, stock.getReservedQty());
        verify(reservationRepository).save(any(InventoryReservationEntity.class));
    }

    @Test
    void reserve_rejectsWhenInsufficient() {
        when(stockRepository.findByVariantId(1L)).thenReturn(Optional.of(stock(2, 0)));

        assertThrows(IllegalArgumentException.class,
                () -> service().reserve(1L, 3, "ORD-1:1", OffsetDateTime.now().plusHours(1), "Rice"));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void consume_dropsReservedQuantity() {
        InventoryStockEntity stock = stock(7, 3);
        InventoryReservationEntity reservation = activeReservation(3);
        when(reservationRepository.findByReservationKeyStartingWithAndStatus("ORD-1:", "ACTIVE"))
                .thenReturn(List.of(reservation));
        when(stockRepository.findByVariantId(1L)).thenReturn(Optional.of(stock));

        service().consume("ORD-1:");

        assertEquals("CONSUMED", reservation.getStatus());
        assertEquals(0, stock.getReservedQty());
        assertEquals(7, stock.getAvailableQty());
    }

    @Test
    void release_returnsQuantityToAvailable() {
        InventoryStockEntity stock = stock(7, 3);
        InventoryReservationEntity reservation = activeReservation(3);
        when(reservationRepository.findByReservationKeyStartingWithAndStatus("ORD-1:", "ACTIVE"))
                .thenReturn(List.of(reservation));
        when(stockRepository.findByVariantId(1L)).thenReturn(Optional.of(stock));

        service().release("ORD-1:");

        assertEquals("RELEASED", reservation.getStatus());
        assertEquals(10, stock.getAvailableQty());
        assertEquals(0, stock.getReservedQty());
    }
}
