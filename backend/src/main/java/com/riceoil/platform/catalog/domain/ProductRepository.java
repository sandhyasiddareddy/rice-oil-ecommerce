package com.riceoil.platform.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findBySlug(String slug);

    List<ProductEntity> findByPublishedTrueOrderByNameAsc();

    List<ProductEntity> findByPublishedTrueAndCategorySlugIgnoreCaseOrderByNameAsc(String categorySlug);

    List<ProductEntity> findByPublishedTrueAndNameContainingIgnoreCaseOrderByNameAsc(String query);
}
