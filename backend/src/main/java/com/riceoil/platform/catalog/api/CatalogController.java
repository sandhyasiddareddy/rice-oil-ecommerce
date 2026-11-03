package com.riceoil.platform.catalog.api;

import com.riceoil.platform.catalog.application.CatalogCategoryView;
import com.riceoil.platform.catalog.application.CatalogProductCardView;
import com.riceoil.platform.catalog.application.CatalogProductDetailView;
import com.riceoil.platform.catalog.application.CatalogQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogQueryService catalogQueryService;

    public CatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping("/categories")
    public List<CatalogCategoryView> categories() {
        return catalogQueryService.listCategories();
    }

    @GetMapping("/products")
    public List<CatalogProductCardView> products(@RequestParam(required = false) String category) {
        return catalogQueryService.listProducts(category);
    }

    @GetMapping("/products/{slugOrSku}")
    public CatalogProductDetailView product(@PathVariable String slugOrSku) {
        return catalogQueryService.getProduct(slugOrSku);
    }

    @GetMapping("/search")
    public List<CatalogProductCardView> search(@RequestParam(name = "q") String query) {
        return catalogQueryService.searchProducts(query);
    }
}
