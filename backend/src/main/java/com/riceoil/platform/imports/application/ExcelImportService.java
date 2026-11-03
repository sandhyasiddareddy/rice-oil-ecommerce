package com.riceoil.platform.imports.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.PackUnit;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductImageEntity;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.imports.domain.ImportAuditLogEntity;
import com.riceoil.platform.imports.domain.ImportAuditLogRepository;
import com.riceoil.platform.imports.domain.ImportJobEntity;
import com.riceoil.platform.imports.domain.ImportJobRepository;
import com.riceoil.platform.imports.domain.ImportJobStatus;
import com.riceoil.platform.imports.domain.ImportRowResultEntity;
import com.riceoil.platform.imports.domain.ImportRowResultRepository;
import com.riceoil.platform.imports.domain.ImportRowStatus;
import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.shared.web.DuplicateResourceException;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class ExcelImportService {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
    private static final String INR = "INR";

    private static final List<String> REQUIRED_HEADERS = List.of(
            "SKU", "Product Name", "Category", "Description", "Pack Size", "Unit",
            "Regular Price", "Stock Quantity", "Image URL", "Published"
    );

    private final ImportJobRepository importJobRepository;
    private final ImportRowResultRepository importRowResultRepository;
    private final ImportAuditLogRepository importAuditLogRepository;
    private final ProductVariantRepository productVariantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryCommandService inventoryCommandService;
    private final ObjectMapper objectMapper;

    public ExcelImportService(ImportJobRepository importJobRepository,
                              ImportRowResultRepository importRowResultRepository,
                              ImportAuditLogRepository importAuditLogRepository,
                              ProductVariantRepository productVariantRepository,
                              CategoryRepository categoryRepository,
                              ProductRepository productRepository,
                              ProductImageRepository productImageRepository,
                              InventoryCommandService inventoryCommandService,
                              ObjectMapper objectMapper) {
        this.importJobRepository = importJobRepository;
        this.importRowResultRepository = importRowResultRepository;
        this.importAuditLogRepository = importAuditLogRepository;
        this.productVariantRepository = productVariantRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryCommandService = inventoryCommandService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ImportPreviewResult preview(MultipartFile file, String initiatedBy, String idempotencyKey) {
        validateFile(file);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<ImportJobEntity> existing = importJobRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existing.isPresent()) {
                ImportJobEntity job = existing.get();
                return new ImportPreviewResult(job.getId(), job.getTotalRows(), job.getValidRows(), job.getInvalidRows());
            }
        }

        ImportJobEntity job = new ImportJobEntity();
        job.setIdempotencyKey(idempotencyKey == null || idempotencyKey.isBlank() ? null : idempotencyKey.trim());
        job.setStatus(ImportJobStatus.PREVIEW_READY);
        job.setFileName(file.getOriginalFilename() == null ? "unknown.xlsx" : file.getOriginalFilename());
        job.setInitiatedBy(initiatedBy);

        DataFormatter formatter = new DataFormatter();
        List<ImportRowResultEntity> rowResults = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();

        int total = 0;
        int valid = 0;
        int invalid = 0;

        try (InputStream inputStream = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Workbook must contain at least one sheet");
            }

            validateHeaders(sheet.getRow(0), formatter);

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (isEmptyRow(row, formatter)) {
                    continue;
                }

                total++;
                int rowNumber = rowIdx + 1;
                ImportRowResultEntity result = new ImportRowResultEntity();
                result.setRowNumber(rowNumber);

                try {
                    ImportRowPayload payload = parsePayload(row, formatter);
                    validatePayload(payload, seenSkus);

                    result.setStatus(ImportRowStatus.VALID);
                    result.setPayloadJson(objectMapper.writeValueAsString(payload));
                    valid++;
                } catch (Exception ex) {
                    result.setStatus(ImportRowStatus.INVALID);
                    result.setErrorCode("ROW_VALIDATION_FAILED");
                    result.setErrorMessage(ex.getMessage());
                    invalid++;
                }

                rowResults.add(result);
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read workbook", ex);
        }

        job.setTotalRows(total);
        job.setValidRows(valid);
        job.setInvalidRows(invalid);
        job.setSummaryJson(buildSummary(total, valid, invalid));
        ImportJobEntity savedJob = importJobRepository.save(job);

        for (ImportRowResultEntity rowResult : rowResults) {
            rowResult.setJob(savedJob);
        }
        importRowResultRepository.saveAll(rowResults);

        saveAudit(savedJob, "PREVIEW_CREATED", initiatedBy,
                "rows=" + total + ", valid=" + valid + ", invalid=" + invalid);

        return new ImportPreviewResult(savedJob.getId(), total, valid, invalid);
    }

    @Transactional
    public ImportApplyResult confirm(Long jobId, String actor) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        if (job.getStatus() != ImportJobStatus.PREVIEW_READY) {
            throw new IllegalArgumentException("Import job is not in PREVIEW_READY state");
        }
        if (importRowResultRepository.existsByJobIdAndStatus(jobId, ImportRowStatus.INVALID)) {
            throw new IllegalArgumentException("Import job has invalid rows and cannot be confirmed");
        }

        List<ImportRowResultEntity> rows = importRowResultRepository.findByJobIdOrderByRowNumberAsc(jobId);

        int createdCategories = 0;
        int createdProducts = 0;
        int createdVariants = 0;

        for (ImportRowResultEntity row : rows) {
            if (row.getStatus() != ImportRowStatus.VALID) {
                continue;
            }
            ImportRowPayload payload = readPayload(row.getPayloadJson());

            CategoryLookup categoryLookup = findOrCreateCategory(payload.category());
            if (categoryLookup.created()) {
                createdCategories++;
            }

            ProductEntity product = createProduct(payload, categoryLookup.category());
            createdProducts++;

            ProductVariantEntity variant = createVariant(payload, product);
            createdVariants++;

            inventoryCommandService.ensureStockRecord(variant.getId());
            if (payload.stockQuantity() > 0) {
                inventoryCommandService.adjustStock(
                        variant.getId(),
                        payload.stockQuantity(),
                        "IMPORT_INITIAL_STOCK",
                        "import-job-" + jobId,
                        actor
                );
            }

            if (payload.imageUrl() != null && !payload.imageUrl().isBlank()) {
                ProductImageEntity image = new ProductImageEntity();
                image.setProduct(product);
                image.setImageUrl(payload.imageUrl());
                image.setAltText(payload.productName());
                image.setSortOrder(0);
                image.setPublished(false);
                productImageRepository.save(image);
            }
        }

        job.setStatus(ImportJobStatus.APPLIED);
        job.setConfirmedAt(OffsetDateTime.now());
        importJobRepository.save(job);

        saveAudit(job, "CONFIRMED", actor,
                "appliedRows=" + job.getValidRows() + ", createdVariants=" + createdVariants);

        return new ImportApplyResult(job.getId(), job.getValidRows(), createdCategories, createdProducts, createdVariants);
    }

    @Transactional(readOnly = true)
    public String buildErrorCsv(Long jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Import job not found: " + jobId));

        List<ImportRowResultEntity> rows = importRowResultRepository.findByJobIdOrderByRowNumberAsc(job.getId());

        StringBuilder csv = new StringBuilder();
        csv.append("Row,Error Code,Error Message\n");
        for (ImportRowResultEntity row : rows) {
            if (row.getStatus() == ImportRowStatus.INVALID) {
                csv.append(row.getRowNumber()).append(',')
                        .append(escapeCsv(row.getErrorCode())).append(',')
                        .append(escapeCsv(row.getErrorMessage())).append('\n');
            }
        }
        return csv.toString();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File exceeds maximum size of 5MB");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".xlsx")) {
            throw new IllegalArgumentException("Only .xlsx files are supported");
        }
    }

    private void validateHeaders(Row header, DataFormatter formatter) {
        if (header == null) {
            throw new IllegalArgumentException("Header row is missing");
        }
        for (int i = 0; i < REQUIRED_HEADERS.size(); i++) {
            String value = formatter.formatCellValue(header.getCell(i));
            if (!REQUIRED_HEADERS.get(i).equals(value)) {
                throw new IllegalArgumentException("Invalid header at column " + (i + 1) + ": expected " + REQUIRED_HEADERS.get(i));
            }
        }
    }

    private ImportRowPayload parsePayload(Row row, DataFormatter formatter) {
        String sku = read(row, 0, formatter).trim().toUpperCase(Locale.ROOT);
        String productName = read(row, 1, formatter).trim();
        String category = read(row, 2, formatter).trim();
        String description = read(row, 3, formatter).trim();
        BigDecimal packSize = new BigDecimal(read(row, 4, formatter).trim());
        PackUnit unit = PackUnit.valueOf(read(row, 5, formatter).trim().toUpperCase(Locale.ROOT));
        BigDecimal regularPrice = new BigDecimal(read(row, 6, formatter).trim());
        int stockQuantity = Integer.parseInt(read(row, 7, formatter).trim());
        String imageUrl = read(row, 8, formatter).trim();
        boolean published = parseBoolean(read(row, 9, formatter).trim());

        return new ImportRowPayload(
                sku,
                productName,
                category,
                description,
                packSize,
                unit,
                regularPrice,
                stockQuantity,
                imageUrl,
                published
        );
    }

    private void validatePayload(ImportRowPayload payload, Set<String> seenSkus) {
        if (payload.sku() == null || payload.sku().isBlank()) {
            throw new IllegalArgumentException("SKU is required");
        }
        if (!seenSkus.add(payload.sku())) {
            throw new IllegalArgumentException("Duplicate SKU in file: " + payload.sku());
        }
        if (productVariantRepository.findBySku(payload.sku()).isPresent()) {
            throw new IllegalArgumentException("SKU already exists: " + payload.sku());
        }
        if (payload.productName() == null || payload.productName().isBlank()) {
            throw new IllegalArgumentException("Product Name is required");
        }
        if (payload.category() == null || payload.category().isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        if (payload.packSize().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Pack Size must be greater than zero");
        }
        if (payload.regularPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Regular Price must be zero or greater");
        }
        if (payload.stockQuantity() < 0) {
            throw new IllegalArgumentException("Stock Quantity must be zero or greater");
        }
        if (payload.imageUrl() != null && !payload.imageUrl().isBlank()) {
            validateUrl(payload.imageUrl());
        }
        if (payload.published()) {
            throw new IllegalArgumentException("Published=true is not allowed for imports");
        }
    }

    private CategoryLookup findOrCreateCategory(String categoryName) {
        String slug = slugify(categoryName);
        Optional<CategoryEntity> existing = categoryRepository.findBySlug(slug);
        if (existing.isPresent()) {
            return new CategoryLookup(existing.get(), false);
        }

        CategoryEntity category = new CategoryEntity();
        category.setName(categoryName);
        category.setSlug(slug);
        category.setDescription(null);
        category.setActive(true);
        return new CategoryLookup(categoryRepository.save(category), true);
    }

    private ProductEntity createProduct(ImportRowPayload payload, CategoryEntity category) {
        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setName(payload.productName());
        product.setSlug(slugify(payload.productName() + "-" + payload.sku()));
        product.setDescription(payload.description());
        product.setAttributesJson(null);
        product.setPublished(false);
        return productRepository.save(product);
    }

    private ProductVariantEntity createVariant(ImportRowPayload payload, ProductEntity product) {
        if (productVariantRepository.findBySku(payload.sku()).isPresent()) {
            throw new DuplicateResourceException("SKU already exists: " + payload.sku());
        }

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku(payload.sku());
        variant.setPackSize(payload.packSize().setScale(3, RoundingMode.HALF_UP));
        variant.setUnit(payload.unit());
        variant.setWeightGrams(deriveWeightGrams(payload.packSize(), payload.unit()));
        variant.setRegularPrice(payload.regularPrice().setScale(2, RoundingMode.HALF_UP));
        variant.setCurrency(INR);
        variant.setActive(true);
        return productVariantRepository.save(variant);
    }

    private int deriveWeightGrams(BigDecimal packSize, PackUnit unit) {
        return switch (unit) {
            case KG, L -> packSize.multiply(BigDecimal.valueOf(1000)).intValueExact();
            case G, ML -> packSize.intValueExact();
        };
    }

    private ImportRowPayload readPayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, ImportRowPayload.class);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid stored row payload", ex);
        }
    }

    private void saveAudit(ImportJobEntity job, String action, String actor, String details) {
        ImportAuditLogEntity log = new ImportAuditLogEntity();
        log.setJob(job);
        log.setAction(action);
        log.setActor(actor);
        log.setDetails(details);
        importAuditLogRepository.save(log);
    }

    private String buildSummary(int total, int valid, int invalid) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", total);
        summary.put("valid", valid);
        summary.put("invalid", invalid);
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private String read(Row row, int idx, DataFormatter formatter) {
        Cell cell = row == null ? null : row.getCell(idx);
        return formatter.formatCellValue(cell);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private boolean parseBoolean(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("yes") || normalized.equals("1");
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null ||
                    (!uri.getScheme().equalsIgnoreCase("http") && !uri.getScheme().equalsIgnoreCase("https"))) {
                throw new IllegalArgumentException("Image URL must start with http or https");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Image URL is invalid");
        }
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }

    private boolean isEmptyRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int i = 0; i < REQUIRED_HEADERS.size(); i++) {
            if (!read(row, i, formatter).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static List<String> requiredHeaders() {
        return REQUIRED_HEADERS;
    }

    private record CategoryLookup(CategoryEntity category, boolean created) {
    }
}
