package com.riceoil.platform.catalog.application;

import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.PackUnit;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductImageEntity;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantEntity;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.inventory.application.InventoryCommandService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "app.catalog.sample-data", name = "enabled", havingValue = "true")
public class SampleCatalogSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SampleCatalogSeeder.class);
    private static final String SEED_ACTOR = "local-sample-data";

    private static final List<SampleProduct> PRODUCTS = List.of(
            new SampleProduct(
                    "Premium Aged Basmati Rice",
                    "premium-aged-basmati-rice",
                    "Aromatic, long-grain basmati rice for biryani, pulao, and everyday meals. Sample listing for local checkout testing.",
                    "/images/rice/basmati.svg",
                    List.of(
                            new SampleVariant("FARM-BAS-1KG", "1.000", 1000, "279.00", 30),
                            new SampleVariant("FARM-BAS-5KG", "5.000", 5000, "1299.00", 12)
                    )
            ),
            new SampleProduct(
                    "Whole Grain Brown Rice",
                    "whole-grain-brown-rice",
                    "Nutty, fiber-rich whole grain brown rice with a satisfying bite. Sample listing for local checkout testing.",
                    "/images/rice/brown-rice.svg",
                    List.of(
                            new SampleVariant("FARM-BRN-1KG", "1.000", 1000, "199.00", 26),
                            new SampleVariant("FARM-BRN-5KG", "5.000", 5000, "899.00", 10)
                    )
            ),
            new SampleProduct(
                    "Kerala Matta Rice",
                    "kerala-matta-rice",
                    "Distinctive, hearty red rice from Kerala, suited to traditional meals. Sample listing for local checkout testing.",
                    "/images/rice/matta-rice.svg",
                    List.of(
                            new SampleVariant("FARM-MAT-1KG", "1.000", 1000, "149.00", 28),
                            new SampleVariant("FARM-MAT-5KG", "5.000", 5000, "649.00", 9)
                    )
            ),
            new SampleProduct(
                    "Sona Masoori Rice",
                    "sona-masoori-rice",
                    "Light, medium-grain rice with a soft texture for everyday South Indian cooking. Sample listing for local checkout testing.",
                    "/images/rice/sona-masoori.svg",
                    List.of(
                            new SampleVariant("FARM-SON-1KG", "1.000", 1000, "119.00", 32),
                            new SampleVariant("FARM-SON-5KG", "5.000", 5000, "529.00", 11)
                    )
            )
    );

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final InventoryCommandService inventoryCommandService;

    public SampleCatalogSeeder(CategoryRepository categoryRepository,
                               ProductRepository productRepository,
                               ProductVariantRepository productVariantRepository,
                               ProductImageRepository productImageRepository,
                               InventoryCommandService inventoryCommandService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.inventoryCommandService = inventoryCommandService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        CategoryEntity riceCategory = categoryRepository.findBySlug("rice")
                .orElseGet(this::createRiceCategory);
        int createdProducts = 0;

        for (SampleProduct sample : PRODUCTS) {
            if (productRepository.findBySlug(sample.slug()).isPresent()) {
                continue;
            }

            ProductEntity product = new ProductEntity();
            product.setCategory(riceCategory);
            product.setName(sample.name());
            product.setSlug(sample.slug());
            product.setDescription(sample.description());
            product.setAttributesJson("{\"sampleData\":true,\"origin\":\"India\"}");
            product.setPublished(true);
            product = productRepository.save(product);

            ProductImageEntity image = new ProductImageEntity();
            image.setProduct(product);
            image.setImageUrl(sample.imageUrl());
            image.setAltText(sample.name() + " sample product illustration");
            image.setSortOrder(0);
            image.setPublished(true);
            productImageRepository.save(image);

            for (SampleVariant sampleVariant : sample.variants()) {
                ProductVariantEntity variant = new ProductVariantEntity();
                variant.setProduct(product);
                variant.setSku(sampleVariant.sku());
                variant.setPackSize(new BigDecimal(sampleVariant.packSize()));
                variant.setUnit(PackUnit.KG);
                variant.setWeightGrams(sampleVariant.weightGrams());
                variant.setRegularPrice(new BigDecimal(sampleVariant.price()));
                variant.setCurrency("INR");
                variant.setActive(true);
                ProductVariantEntity savedVariant = productVariantRepository.save(variant);

                inventoryCommandService.ensureStockRecord(savedVariant.getId());
                inventoryCommandService.adjustStock(
                        savedVariant.getId(),
                        sampleVariant.quantity(),
                        "LOCAL_SAMPLE_DATA",
                        sampleVariant.sku(),
                        SEED_ACTOR
                );
            }

            createdProducts++;
        }

        logger.info("Local sample catalog ready; created {} rice products.", createdProducts);
    }

    private CategoryEntity createRiceCategory() {
        CategoryEntity category = new CategoryEntity();
        category.setName("Rice");
        category.setSlug("rice");
        category.setDescription("Sample rice varieties for local storefront and checkout testing.");
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private record SampleProduct(String name,
                                String slug,
                                String description,
                                String imageUrl,
                                List<SampleVariant> variants) {
    }

    private record SampleVariant(String sku, String packSize, int weightGrams, String price, int quantity) {
    }
}
