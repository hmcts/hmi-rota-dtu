package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
//import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ProcessingServiceTest {

    @Mock
    AzureBlobService azureBlobService;

    @Mock
    ValidationService validationService;

    @Mock
    ValidationConfiguration validationConfiguration;

    // @Mock
    // ConversionService conversionService;

    @InjectMocks
    private ProcessingService processingService;

    private static final String TEST = "Test";
    private static final String TEST_DATA = "1234";
    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    //TODO enable
    //    @Test
    //    void testProcessFileReturnsTrue() throws IOException, SAXException {
    //        BinaryData testData = BinaryData.fromString(TEST);
    //        byte[] testDataBytes = testData.toBytes();
    //        ObjectMapper mapper = new ObjectMapper();
    //        JsonNode node = mapper.createObjectNode();
    //
    //        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
    //        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
    //        when(azureBlobService.downloadBlob(TEST)).thenReturn(testDataBytes);
    //        when(validationConfiguration.getRotaHmiXsd()).thenReturn("PATH");
    //        when(validationService.isValid(any(), any())).thenReturn(true);
    //        when(conversionService.convertXmlToJson(testDataBytes)).thenReturn(node);
    //
    //        BlobItem blobItem = new BlobItem();
    //        blobItem.setName(TEST);
    //
    //        boolean result = processingService.processFile(blobItem);
    //        assertTrue(result, EXPECTED_MESSAGE);
    //    }

    @Test
    void testProcessFileReturnsFalse() throws IOException, SAXException {
        BinaryData testData = BinaryData.fromString(TEST);
        byte[] testDataBytes = testData.toBytes();

        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(testDataBytes);
        when(validationConfiguration.getRotaHmiXsd()).thenReturn("PATH");
        when(validationService.isValid(any(), any())).thenReturn(false);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        boolean result = processingService.processFile(blobItem);
        assertFalse(result, EXPECTED_MESSAGE);
    }
}
