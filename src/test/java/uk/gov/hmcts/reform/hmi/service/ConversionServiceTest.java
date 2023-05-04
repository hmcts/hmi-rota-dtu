package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.JsonNode;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ConversionServiceTest {

    @InjectMocks
    private ConversionService conversionService;

    private static final String ROTA_VALID_XML = "mocks/rotaValidFile.xml";

    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";
    LogCaptor logCaptor = LogCaptor.forClass(ConversionService.class);

    @Test
    void testConvertXmlToJson() throws IOException {
        try (InputStream rotaXml = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(ROTA_VALID_XML)) {
            byte[] rotaXmlAsByte = rotaXml.readAllBytes();
            JsonNode dataAsJson = conversionService.convertXmlToJson(rotaXmlAsByte);
            assertEquals(
                "CS4035951_LEFT_WINGER",
                dataAsJson.get("schedules").get("schedule").get(0).get("id").asText(), EXPECTED_MESSAGE);
        }
    }

    @Test
    void testConvertXmlToJsonException() {
        BinaryData testData = BinaryData.fromString("TEST");
        byte[] testDataBytes = testData.toBytes();
        JsonNode dataAsJson = conversionService.convertXmlToJson(testDataBytes);
        assertEquals(null, dataAsJson, EXPECTED_MESSAGE);
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Failed to convert the blob to Json with error message"),
                   "Error log did not contain message");

    }

    //TODO createRequestJson
    //TODO formatRequestJson
}
