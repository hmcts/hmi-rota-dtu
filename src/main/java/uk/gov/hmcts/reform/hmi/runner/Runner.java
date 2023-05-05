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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Predicate;

@Service
@Slf4j
public class Runner implements CommandLineRunner {

    private final AzureBlobService azureBlobService;
    private final DistributionService distributionService;
    private final ProcessingService processingService;

    @Autowired
    public Runner(AzureBlobService azureBlobService, DistributionService distributionService,
                  ProcessingService processingService) {
        this.azureBlobService = azureBlobService;
        this.distributionService = distributionService;
        this.processingService = processingService;
    }

    @Override
    public void run(String... args) throws IOException, SAXException {
        List<BlobItem> listOfBlobs = azureBlobService.getBlobs();
        log.info("All blobs retrieved");

        // Select an unlocked blob
        Predicate<BlobItem> isUnlocked = blob -> blob.getProperties().getLeaseStatus().equals(LeaseStatusType.UNLOCKED);
        Optional<BlobItem> blobToProcess =  listOfBlobs.stream().filter(isUnlocked).findFirst();

        if (blobToProcess.isPresent()) {
            log.info("Eligible blob selected to process");
            BlobItem blob = blobToProcess.get();

            //Process the selected blob
            processingService.processFile(blob).forEach((key, value) -> {
                Future<Boolean> response = distributionService.sendProcessedJson(value);
                Boolean responseStatus = null;
                try {
                    responseStatus = response.get();
                } catch (InterruptedException | ExecutionException e) {
                    log.error("Async issue. Raise SNOW"); //TODO for SNOW
                    Thread.currentThread().interrupt();
                }

                if (!responseStatus) {
                    log.info("Blob failed");
                    //TODO store in database for SNOW
                }
            });

            // Delete the processed file as we no longer need it
            azureBlobService.deleteProcessingBlob(blob.getName());
            log.info("Blob processed, shutting down");
        }
    }
}
