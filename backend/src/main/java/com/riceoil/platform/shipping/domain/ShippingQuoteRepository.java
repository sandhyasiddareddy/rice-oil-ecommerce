package com.riceoil.platform.shipping.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingQuoteRepository extends JpaRepository<ShippingQuoteEntity, Long> {

    Optional<ShippingQuoteEntity> findByQuoteCode(String quoteCode);
}
