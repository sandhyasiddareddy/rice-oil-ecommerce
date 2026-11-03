package com.riceoil.platform.imports.api.admin;

import com.riceoil.platform.imports.application.ExcelImportService;
import com.riceoil.platform.imports.application.ExcelTemplateService;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminImportsController.class)
@Import(SecurityConfig.class)
class AdminImportsControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExcelTemplateService excelTemplateService;

    @MockitoBean
    private ExcelImportService excelImportService;

    @Test
    void unauthenticatedTemplateRequest_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/imports/template"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedPreviewRequest_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/admin/imports/preview")
                        .param("initiatedBy", "admin@example.com"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedConfirmRequest_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/imports/1/confirm")
                        .contentType("application/json")
                        .content("{\"actor\":\"ops@example.com\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void authenticatedNonAdmin_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/imports/template"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void authenticatedAdmin_shouldSucceed() throws Exception {
        when(excelTemplateService.buildTemplate()).thenReturn("xlsx".getBytes());

        mockMvc.perform(get("/api/admin/imports/template"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=catalog-template.xlsx"));
    }
}
