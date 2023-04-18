package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.LeaseStatusType;
import com.azure.storage.blob.specialized.BlobLeaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;

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

    @Override
    public void run(String... args) {
        List<BlobItem> listOfBlobs = azureBlobService.getBlobs();

        // Select an unlocked blob
        Predicate<BlobItem> isUnlocked = blob -> blob.getProperties().getLeaseStatus().equals(LeaseStatusType.UNLOCKED);
        Optional<BlobItem> blobToProcess =  listOfBlobs.stream().filter(isUnlocked).findFirst();

        // Can refactor don't need if
        // Need to handle retry logic in here if we select a blob thats been taken
        if (blobToProcess.isPresent()) {
            BlobItem blob = blobToProcess.get();
            // Lease it for 60 seconds
            BlobLeaseClient leaseClient = azureBlobService.acquireBlobLease(blob.getName());
            // Break the lease and copy blob for processing
            azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseClient.getLeaseId());
            // Delete the original blob
            azureBlobService.deleteOriginalBlob(blob.getName());
            // Process the file (STUBS FOR NOW)
            distributionService.sendBlobName(blob.getName());
            azureBlobService.deleteProcessingBlob(blob.getName());
        }
    }
}
