package com.riceoil.platform.imports.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, Long> {

    Optional<ImportJobEntity> findByIdempotencyKey(String idempotencyKey);
}
