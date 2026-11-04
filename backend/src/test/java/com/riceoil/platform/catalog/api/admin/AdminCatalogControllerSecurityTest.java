package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.application.CatalogAdminService;
import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminCatalogController.class)
@Import(SecurityConfig.class)
class AdminCatalogControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogAdminService catalogAdminService;

    @Test
    void unauthenticatedCategoryCreate_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/categories")
                        .contentType("application/json")
                        .content("{\"name\":\"Rice\",\"slug\":\"rice\",\"description\":\"desc\",\"active\":true}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void nonAdminCategoryCreate_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/catalog/categories")
                        .contentType("application/json")
                        .content("{\"name\":\"Rice\",\"slug\":\"rice\",\"description\":\"desc\",\"active\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCategoryCreate_shouldSucceed() throws Exception {
        CategoryEntity category = new CategoryEntity();
        setPrivateId(category, 9L);
        category.setName("Rice");
        category.setSlug("rice");
        category.setDescription("desc");
        category.setActive(true);

        when(catalogAdminService.createCategory("Rice", "rice", "desc", true)).thenReturn(category);

        mockMvc.perform(post("/api/admin/catalog/categories")
                        .contentType("application/json")
                        .content("{\"name\":\"Rice\",\"slug\":\"rice\",\"description\":\"desc\",\"active\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.name").value("Rice"))
                .andExpect(jsonPath("$.slug").value("rice"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void configuredBasicAdmin_shouldSucceed() throws Exception {
        CategoryEntity category = new CategoryEntity();
        setPrivateId(category, 10L);
        category.setName("Rice");
        category.setSlug("rice");
        category.setDescription("desc");
        category.setActive(true);
        when(catalogAdminService.createCategory("Rice", "rice", "desc", true)).thenReturn(category);

        mockMvc.perform(post("/api/admin/catalog/categories")
                        .with(httpBasic("admin", "change-me-admin-password"))
                        .contentType("application/json")
                        .content("{\"name\":\"Rice\",\"slug\":\"rice\",\"description\":\"desc\",\"active\":true}"))
                .andExpect(status().isOk());
    }

    private void setPrivateId(Object target, Long id) throws Exception {
        var idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
