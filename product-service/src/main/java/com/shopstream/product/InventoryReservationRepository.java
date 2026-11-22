package com.shopstream.product;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservationRepository
        extends JpaRepository<InventoryReservation, Long> {
}