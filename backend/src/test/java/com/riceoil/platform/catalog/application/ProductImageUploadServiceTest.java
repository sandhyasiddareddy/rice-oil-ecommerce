package com.riceoil.platform.catalog.application;

import com.azure.storage.blob.BlobContainerClient;
import com.riceoil.platform.catalog.domain.ProductEntity;
import com.riceoil.platform.catalog.domain.ProductImageRepository;
import com.riceoil.platform.catalog.domain.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductImageUploadServiceTest {

    @Test
    void uploadWithoutBlobConfiguration_shouldReturnServiceUnavailable() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductImageRepository productImageRepository = mock(ProductImageRepository.class);
        ObjectProvider<BlobContainerClient> blobContainerProvider =
                new StaticListableBeanFactory().getBeanProvider(BlobContainerClient.class);
        when(productRepository.findById(4L)).thenReturn(Optional.of(mock(ProductEntity.class)));

        ProductImageUploadService service = new ProductImageUploadService(
                productRepository, productImageRepository, blobContainerProvider, "");
        MockMultipartFile file = new MockMultipartFile("file", "product.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.upload(4L, file, null));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
