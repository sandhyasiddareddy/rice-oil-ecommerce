package com.riceoil.platform.catalog.application;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductImageEntity;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import com.riceoil.platform.shared.web.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Service
public class ProductImageUploadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageUploadService.class);

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ObjectProvider<BlobContainerClient> blobContainerProvider;
    private final String publicBaseUrl;

    public ProductImageUploadService(ProductRepository productRepository,
                                     ProductImageRepository productImageRepository,
                                     ObjectProvider<BlobContainerClient> blobContainerProvider,
                                     @Value("${app.storage.blob.public-base-url:}") String publicBaseUrl) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.blobContainerProvider = blobContainerProvider;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Transactional
    public ProductImageUploadResult upload(Long productId, MultipartFile file, String altText) {
        if (altText != null && altText.length() > 200) {
            throw new IllegalArgumentException("Image alt text exceeds 200 characters");
        }
        ProductImageFileValidator.ValidatedImage image = ProductImageFileValidator.validate(file);

        ProductEntity product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        BlobContainerClient containerClient = blobContainerProvider.getIfAvailable();
        if (containerClient == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Azure Blob Storage is not configured");
        }
        if (publicBaseUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The public product-image URL is not configured");
        }
        if (!containerClient.exists()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The configured product-image container does not exist");
        }

        String blobName = UUID.randomUUID() + "." + image.extension();
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        ProductImageEntity imageEntity = new ProductImageEntity();
        imageEntity.setProduct(product);
        imageEntity.setImageUrl(publicBaseUrl.replaceAll("/+$", "") + "/" + blobName);
        imageEntity.setAltText(altText);
        imageEntity.setSortOrder(productImageRepository
                .findFirstByProductIdOrderBySortOrderDescIdDesc(productId)
                .map(existing -> existing.getSortOrder() + 1)
                .orElse(0));
        imageEntity.setPublished(true);
        try {
            blobClient.upload(new ByteArrayInputStream(image.content()), image.content().length, false);
            blobClient.setHttpHeaders(new BlobHttpHeaders()
                    .setContentType(image.contentType())
                    .setCacheControl("public, max-age=31536000, immutable"));
            ProductImageEntity savedImage = productImageRepository.saveAndFlush(imageEntity);
            return new ProductImageUploadResult(savedImage.getId(), savedImage.getImageUrl(), savedImage.getAltText());
        } catch (RuntimeException exception) {
            try {
                blobClient.deleteIfExists();
            } catch (RuntimeException cleanupException) {
                LOGGER.warn("Could not remove unreferenced product image blob {}", blobName, cleanupException);
            }
            throw exception;
        }
    }
}
