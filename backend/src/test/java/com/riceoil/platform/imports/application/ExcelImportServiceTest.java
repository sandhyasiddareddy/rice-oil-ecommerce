package com.riceoil.platform.imports.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.catalog.domain.PackUnit;
import com.riceoil.platform.imports.domain.ImportAuditLogRepository;
import com.riceoil.platform.imports.domain.ImportJobEntity;
import com.riceoil.platform.imports.domain.ImportJobRepository;
import com.riceoil.platform.imports.domain.ImportJobStatus;
import com.riceoil.platform.imports.domain.ImportRowResultEntity;
import com.riceoil.platform.imports.domain.ImportRowResultRepository;
import com.riceoil.platform.imports.domain.ImportRowStatus;
import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.shared.web.DuplicateResourceException;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportServiceTest {

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportRowResultRepository importRowResultRepository;

    @Mock
    private ImportAuditLogRepository importAuditLogRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private InventoryCommandService inventoryCommandService;

    private ExcelImportService excelImportService;

    @Test
    void preview_shouldRejectInvalidHeader() throws IOException {
        excelImportService = createService();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bad.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            buildWorkbook(
                headers(),
                new String[][]{}
            )
        );

        assertThrows(IllegalArgumentException.class,
                () -> excelImportService.preview(file, "admin@example.com", null));
    }

    @Test
    void preview_shouldBeIdempotentOnKey() throws Exception {
        ImportJobEntity existing = new ImportJobEntity();
        var idField = ImportJobEntity.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(existing, 42L);
        existing.setTotalRows(10);
        existing.setValidRows(8);
        existing.setInvalidRows(2);

        when(importJobRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        excelImportService = createService();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "ok.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbook(
                        validHeaders(),
                        new String[][]{}
                )
        );

        ImportPreviewResult result = excelImportService.preview(file, "admin@example.com", "key-1");
        assertEquals(42L, result.jobId());
        assertEquals(10, result.totalRows());
        assertEquals(8, result.validRows());
        assertEquals(2, result.invalidRows());
    }

    @Test
    @SuppressWarnings("unchecked")
    void preview_shouldPersistValidAndInvalidRows() throws Exception {
        excelImportService = createService();

        when(productVariantRepository.findBySku(anyString())).thenReturn(Optional.empty());
        when(importJobRepository.save(any(ImportJobEntity.class))).thenAnswer(invocation -> {
            ImportJobEntity job = invocation.getArgument(0);
            setPrivateId(job, 100L);
            return job;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "catalog.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                buildWorkbook(
                        validHeaders(),
                        new String[][]{
                                {"SKU-100", "Wood Pressed Groundnut Oil", "Oils", "Fresh batch", "1", "L", "420", "12", "https://example.com/a.jpg", "false"},
                                {"SKU-101", "Single Origin Rice", "Rice", "Aged", "5", "KG", "520", "5", "https://example.com/b.jpg", "true"}
                        }
                )
        );

        ImportPreviewResult result = excelImportService.preview(file, "admin@example.com", "preview-key-1");

        assertEquals(100L, result.jobId());
        assertEquals(2, result.totalRows());
        assertEquals(1, result.validRows());
        assertEquals(1, result.invalidRows());

        ArgumentCaptor<List<ImportRowResultEntity>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(importRowResultRepository).saveAll(rowsCaptor.capture());

        List<ImportRowResultEntity> savedRows = rowsCaptor.getValue();
        assertEquals(2, savedRows.size());
        assertEquals(ImportRowStatus.VALID, savedRows.get(0).getStatus());
        assertEquals(ImportRowStatus.INVALID, savedRows.get(1).getStatus());
        assertTrue(savedRows.get(1).getErrorMessage().contains("Published=true is not allowed"));
    }

    @Test
    void confirm_shouldRejectWhenInvalidRowsExist() {
        excelImportService = createService();

        ImportJobEntity job = new ImportJobEntity();
        job.setStatus(ImportJobStatus.PREVIEW_READY);

        when(importJobRepository.findById(10L)).thenReturn(Optional.of(job));
        when(importRowResultRepository.existsByJobIdAndStatus(10L, ImportRowStatus.INVALID)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> excelImportService.confirm(10L, "admin@example.com"));
    }

    @Test
    void confirm_shouldNotApplyWhenAlreadyApplied() {
        excelImportService = createService();

        ImportJobEntity job = new ImportJobEntity();
        job.setStatus(ImportJobStatus.APPLIED);
        when(importJobRepository.findById(11L)).thenReturn(Optional.of(job));

        assertThrows(IllegalArgumentException.class, () -> excelImportService.confirm(11L, "admin@example.com"));
        verify(importRowResultRepository, never()).findByJobIdOrderByRowNumberAsc(any());
    }

    @Test
    void confirm_shouldPreventDuplicateVariantCreation() throws Exception {
        excelImportService = createService();

        ImportJobEntity job = new ImportJobEntity();
        setPrivateId(job, 21L);
        job.setStatus(ImportJobStatus.PREVIEW_READY);
        job.setValidRows(1);

        ImportRowResultEntity row = new ImportRowResultEntity();
        row.setRowNumber(2);
        row.setStatus(ImportRowStatus.VALID);
        row.setPayloadJson(new ObjectMapper().writeValueAsString(new ImportRowPayload(
                "SKU-200",
                "Premium Rice",
                "Rice",
                "Aged 12 months",
                new BigDecimal("5"),
                PackUnit.KG,
                new BigDecimal("650"),
                7,
                "https://example.com/rice.jpg",
                false
        )));

        CategoryEntity category = new CategoryEntity();
        category.setName("Rice");
        category.setSlug("rice");
        category.setActive(true);

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setName("Premium Rice");
        product.setSlug("premium-rice-sku-200");
        product.setPublished(false);

        when(importJobRepository.findById(21L)).thenReturn(Optional.of(job));
        when(importRowResultRepository.existsByJobIdAndStatus(21L, ImportRowStatus.INVALID)).thenReturn(false);
        when(importRowResultRepository.findByJobIdOrderByRowNumberAsc(21L)).thenReturn(List.of(row));
        when(categoryRepository.findBySlug("rice")).thenReturn(Optional.of(category));
        when(productRepository.save(any(ProductEntity.class))).thenReturn(product);
        when(productVariantRepository.findBySku("SKU-200")).thenReturn(Optional.of(new ProductVariantEntity()));

        assertThrows(DuplicateResourceException.class, () -> excelImportService.confirm(21L, "admin@example.com"));
        verify(inventoryCommandService, never()).ensureStockRecord(any());
        verify(inventoryCommandService, never()).adjustStock(any(), any(Integer.class), anyString(), anyString(), anyString());
    }

    @Test
    void buildErrorCsv_shouldIncludeOnlyInvalidRowsAndEscapeValues() throws Exception {
        excelImportService = createService();

        ImportJobEntity job = new ImportJobEntity();
        setPrivateId(job, 31L);

        ImportRowResultEntity invalid = new ImportRowResultEntity();
        invalid.setRowNumber(4);
        invalid.setStatus(ImportRowStatus.INVALID);
        invalid.setErrorCode("ROW_VALIDATION_FAILED");
        invalid.setErrorMessage("bad \"url\", must be http/https");

        ImportRowResultEntity valid = new ImportRowResultEntity();
        valid.setRowNumber(5);
        valid.setStatus(ImportRowStatus.VALID);

        when(importJobRepository.findById(31L)).thenReturn(Optional.of(job));
        when(importRowResultRepository.findByJobIdOrderByRowNumberAsc(31L)).thenReturn(List.of(invalid, valid));

        String csv = excelImportService.buildErrorCsv(31L);

        assertTrue(csv.startsWith("Row,Error Code,Error Message"));
        assertTrue(csv.contains("4,\"ROW_VALIDATION_FAILED\",\"bad \"\"url\"\", must be http/https\""));
        assertTrue(!csv.contains("5,"));
    }

    private ExcelImportService createService() {
        return new ExcelImportService(
                importJobRepository,
                importRowResultRepository,
                importAuditLogRepository,
                productVariantRepository,
                categoryRepository,
                productRepository,
                productImageRepository,
                inventoryCommandService,
                new ObjectMapper()
        );
    }

    private String[] headers() {
        return new String[]{"BAD", "Product Name", "Category", "Description", "Pack Size", "Unit",
                "Regular Price", "Stock Quantity", "Image URL", "Published"};
    }

    private String[] validHeaders() {
        return new String[]{"SKU", "Product Name", "Category", "Description", "Pack Size", "Unit",
                "Regular Price", "Stock Quantity", "Image URL", "Published"};
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }

    private byte[] buildWorkbook(String[] headers, String[][] rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("catalog_import");
            var header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            for (int rowIdx = 0; rowIdx < rows.length; rowIdx++) {
                var row = sheet.createRow(rowIdx + 1);
                String[] values = rows[rowIdx];
                for (int colIdx = 0; colIdx < values.length; colIdx++) {
                    row.createCell(colIdx).setCellValue(values[colIdx]);
                }
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}
