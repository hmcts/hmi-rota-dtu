package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.LeaseStatusType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Service
@Slf4j
public class Runner implements CommandLineRunner {

    @Autowired
    AzureBlobService azureBlobService;

    @Autowired
    DistributionService distributionService;

    @Autowired
    ProcessingService processingService;

    @Override
    public void run(String... args) throws IOException, SAXException {
        List<BlobItem> listOfBlobs = azureBlobService.getBlobs();
        log.info("All blobs retrieved");

        // Select an unlocked blob
        Predicate<BlobItem> isUnlocked = blob -> blob.getProperties().getLeaseStatus().equals(LeaseStatusType.UNLOCKED);
        Optional<BlobItem> blobToProcess =  listOfBlobs.stream().filter(isUnlocked).findFirst();

        // Can refactor don't need if
        // Need to handle retry logic in here if we select a blob thats been taken
        if (blobToProcess.isPresent()) {
            log.info("Eligible blob selected to process");
            BlobItem blob = blobToProcess.get();
            //Process the selected blob
            processingService.processFile(blob);
            // Delete the original blob
            azureBlobService.deleteOriginalBlob(blob.getName());
            // Process the file (STUBS FOR NOW)
            distributionService.sendBlobName(blob.getName());
            azureBlobService.deleteProcessingBlob(blob.getName());
            log.info("Blob processed, shutting down");
        }
    }
}
