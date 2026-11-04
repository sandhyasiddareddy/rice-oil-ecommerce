package com.riceoil.platform.customers.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, Long> {
    void deleteByCustomer_IdAndUsedAtIsNull(Long customerId);

    Optional<PasswordResetTokenEntity> findByTokenHashAndUsedAtIsNullAndExpiresAtAfter(
            String tokenHash, OffsetDateTime now);
}
