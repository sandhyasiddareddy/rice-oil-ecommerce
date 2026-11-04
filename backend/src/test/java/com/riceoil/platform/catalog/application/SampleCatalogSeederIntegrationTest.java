package com.riceoil.platform.catalog.application;

import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.inventory.domain.InventoryStockRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
    "app.catalog.sample-data.enabled=true",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:sample-catalog;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SampleCatalogSeederIntegrationTest {

    @Autowired
    private CatalogQueryService catalogQueryService;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private InventoryStockRepository inventoryStockRepository;

    @Test
    void seedsProductsImagesPackSizesAndAvailableStock() {
        var products = catalogQueryService.listProducts("rice");

        assertEquals(4, products.size());
        assertNotNull(products.getFirst().imageUrl());

        var basmati = catalogQueryService.getProduct("premium-aged-basmati-rice");
        assertEquals(2, basmati.variants().size());

        var oneKilogram = productVariantRepository.findBySku("FARM-BAS-1KG").orElseThrow();
        var stock = inventoryStockRepository.findByVariantId(oneKilogram.getId()).orElseThrow();
        assertEquals(30, stock.getAvailableQty());
    }
}
