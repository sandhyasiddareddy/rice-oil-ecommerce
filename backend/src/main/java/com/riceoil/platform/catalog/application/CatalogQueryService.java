package com.riceoil.platform.catalog.application;

import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CatalogQueryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    public CatalogQueryService(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               ProductVariantRepository productVariantRepository,
                               ProductImageRepository productImageRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public List<CatalogCategoryView> listCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(category -> new CatalogCategoryView(
                        category.getId(),
                        category.getName(),
                        category.getSlug(),
                        category.getDescription()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CatalogProductCardView> listProducts(String categorySlug) {
        List<ProductEntity> products = (categorySlug == null || categorySlug.isBlank())
                ? productRepository.findByPublishedTrueOrderByNameAsc()
                : productRepository.findByPublishedTrueAndCategorySlugIgnoreCaseOrderByNameAsc(categorySlug.trim());

        return products.stream()
                .map(this::toProductCard)
                .toList();
    }

    @Transactional(readOnly = true)
    public CatalogProductDetailView getProduct(String slugOrSku) {
        ProductEntity bySlug = productRepository.findBySlug(slugOrSku).orElse(null);
        ProductEntity product = bySlug;

        if (product == null) {
            ProductVariantEntity variant = productVariantRepository.findBySku(slugOrSku).orElse(null);
            if (variant != null) {
                product = variant.getProduct();
            }
        }

        if (product == null || !product.isPublished()) {
            throw new ResourceNotFoundException("Product not found: " + slugOrSku);
        }

        List<CatalogVariantView> variants = productVariantRepository
                .findByProductIdAndActiveTrueOrderByWeightGramsAsc(product.getId())
                .stream()
                .map(this::toVariantView)
                .toList();

        return new CatalogProductDetailView(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getCategory().getSlug(),
                productImageRepository.findFirstByProductIdAndPublishedTrueOrderBySortOrderAsc(product.getId())
                        .map(image -> image.getImageUrl())
                        .orElse(null),
                variants
        );
    }

    @Transactional(readOnly = true)
    public List<CatalogProductCardView> searchProducts(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return productRepository.findByPublishedTrueAndNameContainingIgnoreCaseOrderByNameAsc(query.trim())
                .stream()
                .map(this::toProductCard)
                .toList();
    }

    private CatalogProductCardView toProductCard(ProductEntity product) {
        List<ProductVariantEntity> variants = productVariantRepository.findByProductIdAndActiveTrueOrderByWeightGramsAsc(product.getId());
        BigDecimal startingPrice = variants.isEmpty() ? BigDecimal.ZERO : variants.get(0).getRegularPrice();
        String currency = variants.isEmpty() ? "INR" : variants.get(0).getCurrency();
        return new CatalogProductCardView(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getCategory().getSlug(),
                product.getDescription(),
                startingPrice,
                currency,
                productImageRepository.findFirstByProductIdAndPublishedTrueOrderBySortOrderAsc(product.getId())
                        .map(image -> image.getImageUrl())
                        .orElse(null)
        );
    }

    private CatalogVariantView toVariantView(ProductVariantEntity variant) {
        return new CatalogVariantView(
                variant.getId(),
                variant.getSku(),
                variant.getPackSize(),
                variant.getUnit().name(),
                variant.getWeightGrams(),
                variant.getRegularPrice(),
                variant.getCurrency()
        );
    }
}
