package com.riceoil.platform.pricing.application;

import com.riceoil.platform.pricing.domain.TaxRuleEntity;
import com.riceoil.platform.pricing.domain.TaxRuleRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TaxRuleImportServiceTest {

    private final TaxRuleRepository repository = mock(TaxRuleRepository.class);
    private final TaxRuleImportService service = new TaxRuleImportService(repository);

    private MockMultipartFile workbook(String[]... rows) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = wb.createSheet();
            String[] header = {"code", "name", "ratePercent", "inclusive", "active", "effectiveFrom", "effectiveTo"};
            var h = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                h.createCell(i).setCellValue(header[i]);
            }
            for (int r = 0; r < rows.length; r++) {
                var row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
            return new MockMultipartFile("file", "tax.xlsx", "application/octet-stream", out.toByteArray());
        }
    }

    @Test
    void appliesValidRowsAndUpsertsByCodeAndEffectiveFrom() throws Exception {
        when(repository.findByCodeAndEffectiveFrom(any(), any())).thenReturn(Optional.empty());

        var result = service.importRules(
                workbook(new String[]{"gst0", "No GST", "0", "true", "true", "2026-01-01", ""}), false);

        assertTrue(result.applied());
        assertEquals(1, result.created());
        ArgumentCaptor<TaxRuleEntity> captor = ArgumentCaptor.forClass(TaxRuleEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("GST0", captor.getValue().getCode());
        assertEquals(0, captor.getValue().getRatePercent().compareTo(BigDecimal.ZERO));
    }

    @Test
    void invalidRowBlocksWholeUploadAndDryRunWritesNothing() throws Exception {
        var bad = workbook(
                new String[]{"GST5", "GST 5", "5", "true", "true", "2026-01-01", ""},
                new String[]{"GST9", "Bad", "abc", "maybe", "true", "01/01/2026", ""});

        var result = service.importRules(bad, false);

        assertFalse(result.applied());
        assertEquals(2, result.totalRows());
        assertTrue(result.errors().stream().allMatch(e -> e.row() == 3));
        verify(repository, never()).save(any());

        var dry = service.importRules(
                workbook(new String[]{"GST5", "GST 5", "5", "true", "true", "2026-01-01", ""}), true);
        assertTrue(dry.errors().isEmpty());
        verify(repository, never()).save(any());
    }
}
