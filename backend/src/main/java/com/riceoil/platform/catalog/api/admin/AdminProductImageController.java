package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.application.ProductImageUploadResult;
import com.riceoil.platform.catalog.application.ProductImageUploadService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/catalog/products")
public class AdminProductImageController {

    private final ProductImageUploadService productImageUploadService;

    public AdminProductImageController(ProductImageUploadService productImageUploadService) {
        this.productImageUploadService = productImageUploadService;
    }

    @PostMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductImageUploadResponse uploadImage(@PathVariable Long productId,
                                                  @RequestPart("file") MultipartFile file,
                                                  @RequestParam(required = false) String altText) {
        ProductImageUploadResult result = productImageUploadService.upload(productId, file, altText);
        return new ProductImageUploadResponse(result.id(), result.imageUrl(), result.altText());
    }
}
