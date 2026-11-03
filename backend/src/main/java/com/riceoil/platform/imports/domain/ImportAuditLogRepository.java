package com.riceoil.platform.imports.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportAuditLogRepository extends JpaRepository<ImportAuditLogEntity, Long> {
}
