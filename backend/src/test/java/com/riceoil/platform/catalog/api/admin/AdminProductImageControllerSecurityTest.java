package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.application.ProductImageUploadResult;
import com.riceoil.platform.catalog.application.ProductImageUploadService;
import com.riceoil.platform.shared.web.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminProductImageController.class)
@Import(SecurityConfig.class)
class AdminProductImageControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductImageUploadService productImageUploadService;

    @Test
    void unauthenticatedUpload_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(multipart("/api/admin/catalog/products/4/images")
                        .file("file", new byte[]{1}))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "operator", roles = "USER")
    void nonAdminUpload_shouldReturnForbidden() throws Exception {
        mockMvc.perform(multipart("/api/admin/catalog/products/4/images")
                        .file("file", new byte[]{1}))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminUpload_shouldReturnImageMetadata() throws Exception {
        when(productImageUploadService.upload(eq(4L), any(), eq("Product image")))
                .thenReturn(new ProductImageUploadResult(12L,
                        "https://images.example.test/product-images/image.png", "Product image"));

        mockMvc.perform(multipart("/api/admin/catalog/products/4/images")
                        .file("file", "image".getBytes())
                        .param("altText", "Product image")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.imageUrl").value(
                        "https://images.example.test/product-images/image.png"))
                .andExpect(jsonPath("$.altText").value("Product image"));
    }
}
