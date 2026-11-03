package com.riceoil.platform.imports.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportRowResultRepository extends JpaRepository<ImportRowResultEntity, Long> {

    List<ImportRowResultEntity> findByJobIdOrderByRowNumberAsc(Long jobId);

    boolean existsByJobIdAndStatus(Long jobId, ImportRowStatus status);
}
