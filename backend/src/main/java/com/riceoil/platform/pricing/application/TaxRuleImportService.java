package com.riceoil.platform.pricing.application;

import com.riceoil.platform.pricing.domain.TaxRuleEntity;
import com.riceoil.platform.pricing.domain.TaxRuleRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Admin-managed tax rules via Excel. The upload is all-or-nothing: any invalid row blocks the apply,
 * and a dry run reports the same errors without changing data. Rows upsert by (code, effectiveFrom).
 */
@Service
public class TaxRuleImportService {

    static final List<String> HEADERS = List.of(
            "code", "name", "ratePercent", "inclusive", "active", "effectiveFrom", "effectiveTo");
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private static final long MAX_ROWS = 500;

    public record RowError(int row, String message) {
    }

    public record ImportResult(boolean dryRun, int totalRows, int created, int updated, List<RowError> errors) {
        public boolean applied() {
            return !dryRun && errors.isEmpty();
        }
    }

    public record TaxRuleView(String code, String name, BigDecimal ratePercent, boolean inclusive, boolean active,
                              OffsetDateTime effectiveFrom, OffsetDateTime effectiveTo) {
    }

    private record ParsedRow(int row, String code, String name, BigDecimal rate, boolean inclusive, boolean active,
                             OffsetDateTime from, OffsetDateTime to) {
    }

    private final TaxRuleRepository repository;

    public TaxRuleImportService(TaxRuleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TaxRuleView> list() {
        return repository.findAllByOrderByCodeAscEffectiveFromDesc().stream()
                .map(r -> new TaxRuleView(r.getCode(), r.getName(), r.getRatePercent(), r.isInclusive(),
                        r.isActive(), r.getEffectiveFrom(), r.getEffectiveTo()))
                .toList();
    }

    public byte[] buildTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("tax_rules");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                header.createCell(i).setCellValue(HEADERS.get(i));
            }
            String[] sample = {"GST0", "No GST", "0", "true", "true", "2026-01-01", ""};
            Row example = sheet.createRow(1);
            for (int i = 0; i < sample.length; i++) {
                example.createCell(i).setCellValue(sample[i]);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to build tax rule template", ex);
        }
    }

    @Transactional
    public ImportResult importRules(MultipartFile file, boolean dryRun) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is required");
        }
        List<RowError> errors = new ArrayList<>();
        List<ParsedRow> rows = new ArrayList<>();
        int total = parse(file, rows, errors);

        int created = 0;
        int updated = 0;
        if (errors.isEmpty()) {
            for (ParsedRow row : rows) {
                boolean exists = repository.findByCodeAndEffectiveFrom(row.code(), row.from()).isPresent();
                if (exists) {
                    updated++;
                } else {
                    created++;
                }
                if (!dryRun) {
                    TaxRuleEntity entity = repository.findByCodeAndEffectiveFrom(row.code(), row.from())
                            .orElseGet(TaxRuleEntity::new);
                    entity.setCode(row.code());
                    entity.setName(row.name());
                    entity.setRatePercent(row.rate());
                    entity.setInclusive(row.inclusive());
                    entity.setActive(row.active());
                    entity.setEffectiveFrom(row.from());
                    entity.setEffectiveTo(row.to());
                    repository.save(entity);
                }
            }
        }
        return new ImportResult(dryRun, total, created, updated, errors);
    }

    private int parse(MultipartFile file, List<ParsedRow> rows, List<RowError> errors) {
        DataFormatter formatter = new DataFormatter();
        try (InputStream in = file.getInputStream(); XSSFWorkbook workbook = new XSSFWorkbook(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                throw new IllegalArgumentException("Sheet is empty");
            }
            for (int i = 0; i < HEADERS.size(); i++) {
                String actual = read(header, i, formatter);
                if (!HEADERS.get(i).equalsIgnoreCase(actual)) {
                    throw new IllegalArgumentException("Column " + (i + 1) + " must be '" + HEADERS.get(i)
                            + "' but was '" + actual + "'. Download the template.");
                }
            }
            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new IllegalArgumentException("At most " + MAX_ROWS + " rows are allowed per upload");
            }
            Set<String> seen = new HashSet<>();
            int total = 0;
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isEmpty(row, formatter)) {
                    continue;
                }
                total++;
                ParsedRow parsed = parseRow(r + 1, row, formatter, errors);
                if (parsed != null) {
                    String key = parsed.code() + "|" + parsed.from();
                    if (!seen.add(key)) {
                        errors.add(new RowError(r + 1, "Duplicate code and effectiveFrom in file"));
                    } else {
                        rows.add(parsed);
                    }
                }
            }
            return total;
        } catch (IOException ex) {
            throw new IllegalArgumentException("File is not a valid .xlsx workbook");
        }
    }

    private ParsedRow parseRow(int rowNumber, Row row, DataFormatter formatter, List<RowError> errors) {
        int before = errors.size();
        String code = read(row, 0, formatter).toUpperCase(Locale.ROOT);
        String name = read(row, 1, formatter);
        if (code.isEmpty() || code.length() > 80) {
            errors.add(new RowError(rowNumber, "code is required (max 80 chars)"));
        }
        if (name.isEmpty() || name.length() > 160) {
            errors.add(new RowError(rowNumber, "name is required (max 160 chars)"));
        }
        BigDecimal rate = null;
        try {
            rate = new BigDecimal(read(row, 2, formatter).replace("%", "").trim());
            if (rate.signum() < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
                errors.add(new RowError(rowNumber, "ratePercent must be between 0 and 100"));
            }
        } catch (NumberFormatException ex) {
            errors.add(new RowError(rowNumber, "ratePercent must be a number"));
        }
        Boolean inclusive = parseBoolean(read(row, 3, formatter));
        Boolean active = parseBoolean(read(row, 4, formatter));
        if (inclusive == null) {
            errors.add(new RowError(rowNumber, "inclusive must be true or false"));
        }
        if (active == null) {
            errors.add(new RowError(rowNumber, "active must be true or false"));
        }
        OffsetDateTime from = parseDate(row.getCell(5), formatter, rowNumber, "effectiveFrom", true, errors);
        OffsetDateTime to = parseDate(row.getCell(6), formatter, rowNumber, "effectiveTo", false, errors);
        if (from != null && to != null && !to.isAfter(from)) {
            errors.add(new RowError(rowNumber, "effectiveTo must be after effectiveFrom"));
        }
        if (errors.size() > before) {
            return null;
        }
        return new ParsedRow(rowNumber, code, name, rate, inclusive, active, from, to);
    }

    private OffsetDateTime parseDate(Cell cell, DataFormatter formatter, int rowNumber, String field,
                                     boolean required, List<RowError> errors) {
        if (cell == null || formatter.formatCellValue(cell).isBlank()) {
            if (required) {
                errors.add(new RowError(rowNumber, field + " is required (yyyy-MM-dd)"));
            }
            return null;
        }
        try {
            LocalDate date;
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                    && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                date = cell.getLocalDateTimeCellValue().toLocalDate();
            } else {
                date = LocalDate.parse(formatter.formatCellValue(cell).trim());
            }
            return date.atStartOfDay(ZONE).toOffsetDateTime();
        } catch (DateTimeParseException ex) {
            errors.add(new RowError(rowNumber, field + " must be a date in yyyy-MM-dd format"));
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true", "yes", "y", "1" -> Boolean.TRUE;
            case "false", "no", "n", "0" -> Boolean.FALSE;
            default -> null;
        };
    }

    private String read(Row row, int index, DataFormatter formatter) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private boolean isEmpty(Row row, DataFormatter formatter) {
        for (int i = 0; i < HEADERS.size(); i++) {
            if (!read(row, i, formatter).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
