package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.exception.AzureException;
import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import java.io.IOException;

@Slf4j
@Service
public class ProcessingService {

    private final ValidationService validationService;

    private final AzureBlobService azureBlobService;

    private final ConversionService conversionService;

    private final ValidationConfiguration validationConfiguration;

    public ProcessingService(ValidationService validationService, AzureBlobService azureBlobService,
                             ConversionService conversionService, ValidationConfiguration validationConfiguration) {
        this.validationService = validationService;
        this.azureBlobService = azureBlobService;
        this.conversionService = conversionService;
        this.validationConfiguration = validationConfiguration;
    }

    public boolean processFile(BlobItem blob) throws IOException, SAXException {

        //MOVE FILE TO PROCESSING CONTAINER
        moveFileToProcessingContainer(blob);

        //READ FILE FROM CONTAINER
        byte[] blobData  = azureBlobService.downloadBlob(blob.getName());
        log.info(String.format("Download blob %s", blob.getName()));

        //VALIDATE XML FILE AGAINST SCHEMA FILE PROVIDED BY ROTA
        boolean isFileValid = validationService.isValid(validationConfiguration.getRotaHmiXsd(), blobData);

        log.info(String.format("Blob %s validation: %s", blob.getName(), isFileValid));

        if (isFileValid) {
            //CONVERT XML TO JSON
            conversionService.convertXmlToJson(blobData);
        } else {
            //RAISE SERVICE NOW REQUEST
            return false;
        }

        return true;
    }

    private void moveFileToProcessingContainer(BlobItem blob) {
        try {
            // Lease it for 60 seconds
            String leaseId = azureBlobService.acquireBlobLease(blob.getName());

            // Break the lease and copy blob for processing
            azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);
        } catch (AzureException ex) {
            log.error(String.format("Failed to move the blob %s to processing container with error message: %s",
                                    blob.getName(), ex.getMessage()));
        }
    }
}
