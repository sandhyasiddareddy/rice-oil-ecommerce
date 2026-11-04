package com.riceoil.platform.pricing.application;

import com.riceoil.platform.pricing.domain.TaxRuleEntity;
import com.riceoil.platform.pricing.domain.TaxRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingQuoteServiceTest {

    @Mock
    private TaxRuleRepository taxRuleRepository;

    @InjectMocks
    private PricingQuoteService pricingQuoteService;

    @Test
    void quote_shouldAddTaxForExclusiveRule() {
        TaxRuleEntity rule = new TaxRuleEntity();
        rule.setCode("GST5");
        rule.setRatePercent(new BigDecimal("5"));
        rule.setInclusive(false);
        rule.setActive(true);

        when(taxRuleRepository.findEffective(org.mockito.ArgumentMatchers.eq("GST5"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(rule));

        PricingQuoteResult result = pricingQuoteService.quote(new BigDecimal("100.00"), "GST5");

        assertEquals(new BigDecimal("5.00"), result.taxAmount());
        assertEquals(new BigDecimal("105.00"), result.totalAmount());
        assertEquals(false, result.taxInclusive());
    }

    @Test
    void quote_shouldExtractTaxForInclusiveRule() {
        TaxRuleEntity rule = new TaxRuleEntity();
        rule.setCode("GST5I");
        rule.setRatePercent(new BigDecimal("5"));
        rule.setInclusive(true);
        rule.setActive(true);

        when(taxRuleRepository.findEffective(org.mockito.ArgumentMatchers.eq("GST5I"), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.of(rule));

        PricingQuoteResult result = pricingQuoteService.quote(new BigDecimal("105.00"), "GST5I");

        assertEquals(new BigDecimal("5.00"), result.taxAmount());
        assertEquals(new BigDecimal("105.00"), result.totalAmount());
        assertEquals(true, result.taxInclusive());
    }

    @Test
    void quote_shouldRejectNegativeBaseAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> pricingQuoteService.quote(new BigDecimal("-1.00"), "GST5"));
    }
}
