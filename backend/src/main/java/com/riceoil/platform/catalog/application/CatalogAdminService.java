package com.riceoil.platform.catalog.application;

import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.PackUnit;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.shared.web.DuplicateResourceException;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

@Service
public class CatalogAdminService {

    private static final String INR = "INR";

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryCommandService inventoryCommandService;

    public CatalogAdminService(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               ProductVariantRepository productVariantRepository,
                               InventoryCommandService inventoryCommandService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.inventoryCommandService = inventoryCommandService;
    }

    @Transactional
    public CategoryEntity createCategory(String name, String slug, String description, boolean active) {
        String normalizedSlug = normalizeSlug(slug);
        categoryRepository.findBySlug(normalizedSlug).ifPresent(existing -> {
            throw new DuplicateResourceException("Category slug already exists: " + normalizedSlug);
        });

        CategoryEntity category = new CategoryEntity();
        category.setName(name.trim());
        category.setSlug(normalizedSlug);
        category.setDescription(description);
        category.setActive(active);
        return categoryRepository.save(category);
    }

    @Transactional
    public ProductEntity createProduct(Long categoryId,
                                       String name,
                                       String slug,
                                       String description,
                                       String attributesJson,
                                       boolean published) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        String normalizedSlug = normalizeSlug(slug);
        productRepository.findBySlug(normalizedSlug).ifPresent(existing -> {
            throw new DuplicateResourceException("Product slug already exists: " + normalizedSlug);
        });

        ProductEntity product = new ProductEntity();
        product.setCategory(category);
        product.setName(name.trim());
        product.setSlug(normalizedSlug);
        product.setDescription(description);
        product.setAttributesJson(attributesJson);
        product.setPublished(published);
        return productRepository.save(product);
    }

    @Transactional
    public ProductVariantEntity createVariant(Long productId,
                                              String sku,
                                              BigDecimal packSize,
                                              PackUnit unit,
                                              int weightGrams,
                                              BigDecimal regularPrice,
                                              boolean active) {
        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        String normalizedSku = sku.trim().toUpperCase(Locale.ROOT);
        productVariantRepository.findBySku(normalizedSku).ifPresent(existing -> {
            throw new DuplicateResourceException("SKU already exists: " + normalizedSku);
        });

        if (packSize.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("packSize must be greater than zero");
        }
        if (weightGrams <= 0) {
            throw new IllegalArgumentException("weightGrams must be greater than zero");
        }
        if (regularPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("regularPrice must be zero or greater");
        }

        ProductVariantEntity variant = new ProductVariantEntity();
        variant.setProduct(product);
        variant.setSku(normalizedSku);
        variant.setPackSize(packSize.setScale(3, RoundingMode.HALF_UP));
        variant.setUnit(unit);
        variant.setWeightGrams(weightGrams);
        variant.setRegularPrice(regularPrice.setScale(2, RoundingMode.HALF_UP));
        variant.setCurrency(INR);
        variant.setActive(active);

        ProductVariantEntity saved = productVariantRepository.save(variant);
        inventoryCommandService.ensureStockRecord(saved.getId());
        return saved;
    }

    private String normalizeSlug(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
