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
import org.springframework.http.HttpStatus;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.models.ApiResponse;
import uk.gov.hmcts.reform.hmi.service.AzureBlobService;
import uk.gov.hmcts.reform.hmi.service.DistributionService;
import uk.gov.hmcts.reform.hmi.service.ProcessingService;
import uk.gov.hmcts.reform.hmi.service.ServiceNowService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.LawOfDemeter")
class RunnerTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    DistributionService distributionService;

    @Mock
    ProcessingService processingService;

    @Mock
    ServiceNowService serviceNowService;

    @InjectMocks
    Runner runner;

    private static final String TEST = "Test";
    private static final String RESPONSE_MESSAGE = "Info logs did not contain expected message";

    @Test
    void testRunnerWithNoEligibleBlobToProcess() throws IOException, SAXException {
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

            assertEquals(1, logCaptor.getInfoLogs().size(),
                       "More info logs than expected"
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobToProcess() throws IOException, SAXException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            Map<String, String> testMap = new ConcurrentHashMap<>();
            testMap.put("test", "test-json-data");

            when(processingService.processFile(blobItem)).thenReturn(testMap);
            when(distributionService.sendProcessedJson(any()))
                .thenReturn(CompletableFuture.completedFuture(
                    new ApiResponse(HttpStatus.OK.value(),"received successfully")));
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

            assertEquals(3, logCaptor.getInfoLogs().size(),
                       "More info logs than expected"
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobFailedRequest() throws IOException, SAXException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            Map<String, String> testMap = new ConcurrentHashMap<>();
            testMap.put("test", "test-json-data");

            when(processingService.processFile(blobItem)).thenReturn(testMap);
            when(distributionService.sendProcessedJson(any()))
                .thenReturn(CompletableFuture.completedFuture(
                    new ApiResponse(HttpStatus.BAD_REQUEST.value(), "source header is missing")));
            when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn("fileDeleted");

            runner.run();
            assertTrue(
                logCaptor.getInfoLogs().get(2).contains("Blob failed"),
                RESPONSE_MESSAGE
            );
        }
    }

    @Test
    void testRunnerWithEligibleBlobFailedHmiRequest() throws IOException, SAXException {
        try (LogCaptor logCaptor = LogCaptor.forClass(Runner.class)) {
            BlobItem blobItem = new BlobItem();
            blobItem.setName(TEST);
            BlobItemProperties blobItemProperties = new BlobItemProperties();
            blobItemProperties.setLeaseStatus(LeaseStatusType.UNLOCKED);
            blobItem.setProperties(blobItemProperties);
            when(azureBlobService.getBlobs()).thenReturn(List.of(blobItem));

            Map<String, String> testMap = new ConcurrentHashMap<>();
            testMap.put("test", "test-json-data");

            when(processingService.processFile(blobItem)).thenReturn(testMap);
            when(distributionService.sendProcessedJson(any()))
                .thenReturn(null);
            when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);
            when(azureBlobService.deleteProcessingBlob(TEST)).thenReturn("fileDeleted");

            runner.run();

            assertTrue(
                logCaptor.getErrorLogs().get(0).contains("Async issue"),
                RESPONSE_MESSAGE
            );
        }
    }
}
