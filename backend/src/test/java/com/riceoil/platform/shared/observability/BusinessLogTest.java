package com.riceoil.platform.shared.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessLogTest {

    @Test
    void formatsKeyValuePairsAndSkipsNulls() {
        assertThat(BusinessLog.format("ORDER_ITEM", "sku", "RICE-1", "quantity", 2, "note", null))
                .isEqualTo("event=ORDER_ITEM sku=RICE-1 quantity=2");
    }

    @Test
    void quotesValuesAndStripsNewlines() {
        assertThat(BusinessLog.format("X", "reason", "bad\nvalue = \"1\""))
                .isEqualTo("event=X reason=\"bad value = \\\"1\\\"\"");
    }

    @Test
    void masksEmailMailbox() {
        assertThat(BusinessLog.maskEmail("jane.doe@example.com")).isEqualTo("j***@example.com");
        assertThat(BusinessLog.maskEmail(null)).isNull();
    }
}
