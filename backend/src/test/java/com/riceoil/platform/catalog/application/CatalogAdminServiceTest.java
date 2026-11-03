package com.riceoil.platform.catalog.application;

import com.riceoil.platform.catalog.domain.CategoryEntity;
import com.riceoil.platform.catalog.domain.CategoryRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.catalog.domain.ProductVariantRepository;
import com.riceoil.platform.inventory.application.InventoryCommandService;
import com.riceoil.platform.shared.web.DuplicateResourceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAdminServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryCommandService inventoryCommandService;

    @InjectMocks
    private CatalogAdminService catalogAdminService;

    @Test
    void createCategory_shouldRejectDuplicateSlug() {
        when(categoryRepository.findBySlug("rice")).thenReturn(Optional.of(new CategoryEntity()));

        assertThrows(DuplicateResourceException.class,
                () -> catalogAdminService.createCategory("Rice", "Rice", "desc", true));
    }
}
