package com.riceoil.platform.imports.api.admin;

import com.riceoil.platform.imports.application.ExcelImportService;
import com.riceoil.platform.imports.application.ExcelTemplateService;
import com.riceoil.platform.imports.application.ImportApplyResult;
import com.riceoil.platform.imports.application.ImportPreviewResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminImportsController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminImportsControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockitoBean
    private ExcelTemplateService excelTemplateService;

        @MockitoBean
    private ExcelImportService excelImportService;

    @Test
    void downloadTemplate_shouldReturnXlsxAttachment() throws Exception {
        byte[] content = "xlsx-data".getBytes();
        when(excelTemplateService.buildTemplate()).thenReturn(content);

        mockMvc.perform(get("/api/admin/imports/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=catalog-template.xlsx"))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(content));
    }

    @Test
    void preview_shouldAcceptMultipartAndReturnSummary() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "catalog.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "fake-data".getBytes()
        );

        when(excelImportService.preview(eq(file), eq("admin@example.com"), eq("idem-123")))
                .thenReturn(new ImportPreviewResult(77L, 10, 9, 1));

        mockMvc.perform(multipart("/api/admin/imports/preview")
                        .file(file)
                        .param("initiatedBy", "admin@example.com")
                        .header("Idempotency-Key", "idem-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(77))
                .andExpect(jsonPath("$.totalRows").value(10))
                .andExpect(jsonPath("$.validRows").value(9))
                .andExpect(jsonPath("$.invalidRows").value(1));
    }

    @Test
    void confirm_shouldReturnApplySummary() throws Exception {
        when(excelImportService.confirm(5L, "ops@example.com"))
                .thenReturn(new ImportApplyResult(5L, 3, 1, 3, 3));

        mockMvc.perform(post("/api/admin/imports/5/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"ops@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(5))
                .andExpect(jsonPath("$.appliedRows").value(3))
                .andExpect(jsonPath("$.createdCategories").value(1))
                .andExpect(jsonPath("$.createdProducts").value(3))
                .andExpect(jsonPath("$.createdVariants").value(3));
    }

    @Test
    void confirm_shouldRejectBlankActor() throws Exception {
        mockMvc.perform(post("/api/admin/imports/5/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadErrors_shouldReturnCsvAttachment() throws Exception {
        when(excelImportService.buildErrorCsv(22L))
                .thenReturn("Row,Error Code,Error Message\n2,\"ROW_VALIDATION_FAILED\",\"Bad value\"\n");

        mockMvc.perform(get("/api/admin/imports/22/errors"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=import-errors-22.csv"))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().string("Row,Error Code,Error Message\n2,\"ROW_VALIDATION_FAILED\",\"Bad value\"\n"));

        verify(excelImportService).buildErrorCsv(22L);
    }
}
