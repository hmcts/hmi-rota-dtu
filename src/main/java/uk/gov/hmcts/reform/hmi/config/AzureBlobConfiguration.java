package uk.gov.hmcts.reform.hmi.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test & !functional")
public class AzureBlobConfiguration {

    @Bean(name = "rota")
    public BlobContainerClient rotaBlobContainerClient(AzureBlobConfigurationProperties
                                                               azureBlobConfigurationProperties) {
        return new BlobContainerClientBuilder()
            .connectionString(azureBlobConfigurationProperties.getConnectionString())
            .containerName(azureBlobConfigurationProperties.getRotaContainerName())
            .buildClient();
    }

    @Bean(name = "processing")
    public BlobContainerClient processingBlobContainerClient(AzureBlobConfigurationProperties
                                                                   azureBlobConfigurationProperties) {
        return new BlobContainerClientBuilder()
            .connectionString(azureBlobConfigurationProperties.getConnectionString())
            .containerName(azureBlobConfigurationProperties.getProcessingContainerName())
            .buildClient();
    }
}
