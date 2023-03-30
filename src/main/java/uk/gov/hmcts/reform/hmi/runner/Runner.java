package uk.gov.hmcts.reform.hmi.runner;


import com.azure.storage.blob.models.BlobItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

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
        Map<String, OffsetDateTime> blobMap = new ConcurrentHashMap<>();

        listOfBlobs.forEach(blob -> blobMap.put(blob.getName(), blob.getProperties().getLastModified()));
        String latestBlob = blobMap.entrySet().stream()
            .max(Entry.comparingByValue())
            .map(Entry::getKey)
            .orElse(null);

        if (latestBlob != null) {
            distributionService.sendBlobName(latestBlob);
            azureBlobService.deleteBlob(latestBlob);
        }
    }
}
