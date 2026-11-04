package com.riceoil.platform.catalog.infrastructure.storage;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureBlobStorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "azure.storage.blob.endpoint")
    BlobContainerClient productImageBlobContainer(
            @Value("${azure.storage.blob.endpoint}") String endpoint,
            @Value("${app.storage.blob.container}") String containerName) {
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient()
                .getBlobContainerClient(containerName);
    }
}
