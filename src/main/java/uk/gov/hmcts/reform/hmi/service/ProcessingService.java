package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ProcessingService {

    private final ValidationService validationService;

    private final AzureBlobService azureBlobService;

    public ProcessingService(ValidationService validationService, AzureBlobService azureBlobService) {
        this.validationService = validationService;
        this.azureBlobService = azureBlobService;
    }

    public boolean processFile(BlobItem blob) throws IOException {
        // Lease it for 60 seconds
        String leaseId = azureBlobService.acquireBlobLease(blob.getName());

        // Break the lease and copy blob for processing
        azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);

        InputStream blobData  = azureBlobService.downloadBlob(blob.getName());

        validationService.isValid(blobData);

        return true;
    }
}
