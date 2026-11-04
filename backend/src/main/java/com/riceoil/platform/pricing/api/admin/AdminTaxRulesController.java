package com.riceoil.platform.pricing.api.admin;

import com.riceoil.platform.pricing.application.TaxRuleImportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/tax-rules")
public class AdminTaxRulesController {

    private final TaxRuleImportService importService;

    public AdminTaxRulesController(TaxRuleImportService importService) {
        this.importService = importService;
    }

    @GetMapping
    public List<TaxRuleImportService.TaxRuleView> list() {
        return importService.list();
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tax-rules-template.xlsx")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(importService.buildTemplate());
    }

    /** dryRun=true validates and reports; dryRun=false applies when every row is valid. */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TaxRuleImportService.ImportResult importRules(@RequestPart("file") MultipartFile file,
                                                         @RequestParam(defaultValue = "true") boolean dryRun) {
        return importService.importRules(file, dryRun);
    }
}
