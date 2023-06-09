package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import nl.altindag.log.LogCaptor;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;
import uk.gov.hmcts.reform.hmi.database.CourtListingProfileRepository;
import uk.gov.hmcts.reform.hmi.database.JusticeRepository;
import uk.gov.hmcts.reform.hmi.database.LocationRepository;
import uk.gov.hmcts.reform.hmi.database.ScheduleRepository;
import uk.gov.hmcts.reform.hmi.database.VenueRepository;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Mock
    ConversionService conversionService;

    @Mock
    JusticeRepository justiceRepository;

    @Mock
    LocationRepository locationRepository;

    @Mock
    VenueRepository venueRepository;

    @Mock
    CourtListingProfileRepository courtListingProfileRepository;

    @Mock
    ScheduleRepository scheduleRepository;

    @Mock
    ServiceNowService serviceNowService;

    @InjectMocks
    private ProcessingService processingService;

    LogCaptor logCaptor = LogCaptor.forClass(ProcessingService.class);

    private static final String TEST = "Test";
    private static final String TEST_DATA = "1234";
    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";
    private static final String DATABASE_EXCEPTION_MESSAGE =
        "Issue processing json into the database";

    @Test
    void testProcessFile() throws IOException, SAXException {
        File file = new File(Thread.currentThread().getContextClassLoader()
                                 .getResource("mocks/rotaValidFile.xml").getFile());


        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        byte[] fileByteArray = FileUtils.readFileToByteArray(file);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(fileByteArray);
        XmlMapper mapper = new XmlMapper();
        Map<String, String> testMap = new ConcurrentHashMap<>();
        testMap.put("1234", "testString");
        when(conversionService.convertXmlToJson(any())).thenReturn(mapper.readTree(fileByteArray));
        when(validationConfiguration.getRotaHmiXsd()).thenReturn("path");
        when(validationService.isValid(any(), any())).thenReturn(true);

        when(justiceRepository.saveAll(any())).thenReturn(List.of());
        when(locationRepository.saveAll(any())).thenReturn(List.of());
        when(venueRepository.saveAll(any())).thenReturn(List.of());
        when(courtListingProfileRepository.saveAll(any())).thenReturn(List.of());
        when(scheduleRepository.saveAll(any())).thenReturn(List.of());
        when(conversionService.createRequestJson()).thenReturn(testMap);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        Map<String, String> result = processingService.processFile(blobItem);
        assertEquals(testMap, result, EXPECTED_MESSAGE);
    }

    @Test
    void testProcessFileReturnsEmpty() throws IOException, SAXException {
        BinaryData testData = BinaryData.fromString(TEST);
        byte[] testDataBytes = testData.toBytes();

        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(testDataBytes);
        when(validationConfiguration.getRotaHmiXsd()).thenReturn("PATH");
        when(validationService.isValid(any(), any())).thenReturn(false);
        when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        Map<String, String> result = processingService.processFile(blobItem);
        assertTrue(result.isEmpty(), EXPECTED_MESSAGE);
    }

    @Test
    void testProcessFileErrorSaveToDatabase() throws IOException, SAXException {
        File file = new File(Thread.currentThread().getContextClassLoader()
                                 .getResource("mocks/rotaInvalidFile.xml").getFile());


        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        byte[] fileByteArray = FileUtils.readFileToByteArray(file);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(fileByteArray);
        XmlMapper mapper = new XmlMapper();
        when(conversionService.convertXmlToJson(any())).thenReturn(mapper.readTree(fileByteArray));
        when(validationConfiguration.getRotaHmiXsd()).thenReturn("path");
        when(validationService.isValid(any(), any())).thenReturn(true);

        when(justiceRepository.saveAll(any())).thenReturn(List.of());
        when(locationRepository.saveAll(any())).thenReturn(List.of());
        when(venueRepository.saveAll(any())).thenReturn(List.of());
        when(courtListingProfileRepository.saveAll(any())).thenReturn(List.of());
        when(serviceNowService.createServiceNowRequest(any(), any())).thenReturn(true);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        Map<String, String> result = processingService.processFile(blobItem);
        assertEquals(new HashMap<>(), result, EXPECTED_MESSAGE);
    }

    @Test
    void testProcessFileErrorSaveJusticesToDatabase() throws IOException, SAXException {
        File file = new File(Thread.currentThread().getContextClassLoader()
                                 .getResource("mocks/rotaInvalidAttributeFile.xml").getFile());

        byte[] fileByteArray = FileUtils.readFileToByteArray(file);
        XmlMapper mapper = new XmlMapper();

        when(azureBlobService.acquireBlobLease(TEST)).thenReturn(TEST_DATA);
        doNothing().when(azureBlobService).copyBlobToProcessingContainer(TEST, TEST_DATA);
        when(azureBlobService.downloadBlob(TEST)).thenReturn(fileByteArray);
        when(conversionService.convertXmlToJson(any())).thenReturn(mapper.readTree(fileByteArray));
        when(validationConfiguration.getRotaHmiXsd()).thenReturn("path");
        when(validationService.isValid(any(), any())).thenReturn(true);

        BlobItem blobItem = new BlobItem();
        blobItem.setName(TEST);

        processingService.processFile(blobItem);
        assertTrue(logCaptor.getErrorLogs().get(0).contains(DATABASE_EXCEPTION_MESSAGE),
                   "Error log did not contain message");
    }
}
