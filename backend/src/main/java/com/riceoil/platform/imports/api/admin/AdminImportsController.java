package com.riceoil.platform.imports.api.admin;

import com.riceoil.platform.imports.application.ExcelImportService;
import com.riceoil.platform.imports.application.ExcelTemplateService;
import com.riceoil.platform.imports.application.ImportApplyResult;
import com.riceoil.platform.imports.application.ImportPreviewResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/imports")
@Validated
public class AdminImportsController {

    private final ExcelTemplateService excelTemplateService;
    private final ExcelImportService excelImportService;

    public AdminImportsController(ExcelTemplateService excelTemplateService,
                                  ExcelImportService excelImportService) {
        this.excelTemplateService = excelTemplateService;
        this.excelImportService = excelImportService;
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = excelTemplateService.buildTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=catalog-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportPreviewResponse preview(
            @RequestPart("file") MultipartFile file,
            @RequestParam("initiatedBy") String initiatedBy,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {

        ImportPreviewResult result = excelImportService.preview(file, initiatedBy, idempotencyKey);
        return new ImportPreviewResponse(result.jobId(), result.totalRows(), result.validRows(), result.invalidRows());
    }

    @PostMapping("/{jobId}/confirm")
    public ImportApplyResponse confirm(@PathVariable Long jobId,
                           @Valid @RequestBody ImportConfirmRequest request) {
        ImportApplyResult result = excelImportService.confirm(jobId, request.actor());
        return new ImportApplyResponse(
                result.jobId(),
                result.appliedRows(),
                result.createdCategories(),
                result.createdProducts(),
                result.createdVariants()
        );
    }

    @GetMapping("/{jobId}/errors")
    public ResponseEntity<String> downloadErrors(@PathVariable Long jobId) {
        String csv = excelImportService.buildErrorCsv(jobId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=import-errors-" + jobId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
