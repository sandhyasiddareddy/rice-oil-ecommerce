package com.riceoil.platform.pricing.application;

import com.riceoil.platform.pricing.domain.TaxRuleEntity;
import com.riceoil.platform.pricing.domain.TaxRuleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Service
public class PricingQuoteService {

    private final TaxRuleRepository taxRuleRepository;

    public PricingQuoteService(TaxRuleRepository taxRuleRepository) {
        this.taxRuleRepository = taxRuleRepository;
    }

    public PricingQuoteResult quote(BigDecimal baseAmount, String taxCode) {
        if (baseAmount == null || baseAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("baseAmount must be zero or greater");
        }
        if (taxCode == null || taxCode.isBlank()) {
            return new PricingQuoteResult(
                    money(baseAmount),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    money(baseAmount),
                    null,
                    false
            );
        }

        Optional<TaxRuleEntity> optionalRule = taxRuleRepository.findFirstByCodeAndActiveTrueOrderByEffectiveFromDesc(
                taxCode.trim().toUpperCase()
        );

        if (optionalRule.isEmpty()) {
            return new PricingQuoteResult(
                    money(baseAmount),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    money(baseAmount),
                    taxCode.trim().toUpperCase(),
                    false
            );
        }

        TaxRuleEntity rule = optionalRule.get();
        BigDecimal normalizedBase = money(baseAmount);

        if (rule.isInclusive()) {
            BigDecimal divisor = BigDecimal.valueOf(100).add(rule.getRatePercent());
            BigDecimal taxable = normalizedBase.multiply(rule.getRatePercent())
                    .divide(divisor, 2, RoundingMode.HALF_UP);
            return new PricingQuoteResult(
                    normalizedBase,
                    taxable,
                    normalizedBase,
                    rule.getCode(),
                    true
            );
        }

        BigDecimal tax = normalizedBase.multiply(rule.getRatePercent())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new PricingQuoteResult(
                normalizedBase,
                tax,
                money(normalizedBase.add(tax)),
                rule.getCode(),
                false
        );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
