package uk.gov.hmcts.reform.hmi.runner;

import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobItemProperties;
import com.azure.storage.blob.models.LeaseStatusType;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunnerTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    DistributionService distributionService;

    @InjectMocks
    Runner runner;

    private static final String TEST = "Test";
    private static final String RESPONSE_MESSAGE = "Info logs did not contain expected message";

    @Test
    void testRunnerWithNoEligibleBlobToProcess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.LOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains("All blobs retrieved"),
                RESPONSE_MESSAGE
            );

            assertTrue(logCaptor.getInfoLogs().size() == 1,
                       "More info logs than expected"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRunnerWithEligibleBlobToProcess() {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));
            when(azureBlobService.acquireBlobLease(TEST)).thenReturn("1234");
            doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, "1234");
            when(azureBlobService.deleteOriginalBlob(TEST)).thenReturn("fileDeleted");
            when(distributionService.sendBlobName(TEST)).thenReturn(true);
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn("fileDeleted");

            runner.run();

            assertTrue(
                logCaptor.getInfoLogs().get(0).contains("All blobs retrieved"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(1).contains("Eligible blob selected to process"),
                RESPONSE_MESSAGE
            );

            assertTrue(
                logCaptor.getInfoLogs().get(2).contains("Blob processed, shutting down"),
                RESPONSE_MESSAGE
            );

            assertTrue(logCaptor.getInfoLogs().size() == 3,
                       "More info logs than expected"
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
