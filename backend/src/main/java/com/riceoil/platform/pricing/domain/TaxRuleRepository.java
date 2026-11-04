package com.riceoil.platform.pricing.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface TaxRuleRepository extends JpaRepository<TaxRuleEntity, Long> {

    Optional<TaxRuleEntity> findFirstByCodeAndActiveTrueOrderByEffectiveFromDesc(String code);

    @Query("select r from TaxRuleEntity r where r.code = :code and r.active = true "
            + "and r.effectiveFrom <= :at and (r.effectiveTo is null or r.effectiveTo > :at) "
            + "order by r.effectiveFrom desc limit 1")
    Optional<TaxRuleEntity> findEffective(@Param("code") String code, @Param("at") OffsetDateTime at);

    Optional<TaxRuleEntity> findByCodeAndEffectiveFrom(String code, OffsetDateTime effectiveFrom);

    List<TaxRuleEntity> findAllByOrderByCodeAscEffectiveFromDesc();
}
