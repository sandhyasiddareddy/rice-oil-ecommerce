package com.riceoil.platform.orders.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuestOrderAccessTokenRepository extends JpaRepository<GuestOrderAccessTokenEntity, Long> {
    Optional<GuestOrderAccessTokenEntity> findByTokenHash(String tokenHash);
}
