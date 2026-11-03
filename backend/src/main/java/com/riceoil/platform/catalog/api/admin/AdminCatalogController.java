package com.riceoil.platform.catalog.api.admin;

import com.riceoil.platform.catalog.application.CatalogAdminService;
import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/catalog")
public class AdminCatalogController {

    private final CatalogAdminService catalogAdminService;

    public AdminCatalogController(CatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @PostMapping("/categories")
    public CategoryResponse createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryEntity category = catalogAdminService.createCategory(
                request.name(),
                request.slug(),
                request.description(),
                request.active()
        );

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.isActive()
        );
    }

    @PostMapping("/products")
    public ProductResponse createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductEntity product = catalogAdminService.createProduct(
                request.categoryId(),
                request.name(),
                request.slug(),
                request.description(),
                request.attributesJson(),
                request.published()
        );

        return new ProductResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getAttributesJson(),
                product.isPublished()
        );
    }

    @PostMapping("/variants")
    public VariantResponse createVariant(@Valid @RequestBody CreateVariantRequest request) {
        ProductVariantEntity variant = catalogAdminService.createVariant(
                request.productId(),
                request.sku(),
                request.packSize(),
                request.unit(),
                request.weightGrams(),
                request.regularPrice(),
                request.active()
        );

        return new VariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getPackSize(),
                variant.getUnit(),
                variant.getWeightGrams(),
                variant.getRegularPrice(),
                variant.getCurrency(),
                variant.isActive()
        );
    }
}
