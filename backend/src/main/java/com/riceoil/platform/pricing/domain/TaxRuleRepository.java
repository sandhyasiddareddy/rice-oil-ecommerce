package com.riceoil.platform.pricing.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

    Optional<TaxRuleEntity> findFirstByCodeAndActiveTrueOrderByEffectiveFromDesc(String code);
}
