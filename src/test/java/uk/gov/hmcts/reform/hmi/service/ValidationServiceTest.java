package uk.gov.hmcts.reform.hmi.service;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ValidationServiceTest {

    @InjectMocks
    private ValidationService validationService;
    private static final String ROTA_XML_SCHEMA = "schemas/rota-hmi-interface.xsd";
    private static final String ROTA_VALID_XML = "mocks/rotaValidFile.xml";
    private static final String ROTA_INVALID_XML = "mocks/rotaInvalidFile.xml";
    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    LogCaptor logCaptor = LogCaptor.forClass(ValidationService.class);

    @Test
    void testIsValidReturnsTrue() throws IOException, SAXException {
        try (InputStream rotaXml = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(ROTA_VALID_XML)) {
            byte[] rotaXmlAsByte = rotaXml.readAllBytes();
            assertTrue(validationService.isValid(ROTA_XML_SCHEMA, rotaXmlAsByte),
                       EXPECTED_MESSAGE);
        }
    }

    @Test
    void testIsValidReturnsFalse() throws IOException, SAXException {
        try (InputStream rotaXml = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(ROTA_INVALID_XML)) {
            byte[] rotaXmlAsByte = rotaXml.readAllBytes();
            assertFalse(validationService.isValid(ROTA_XML_SCHEMA, rotaXmlAsByte),
                       EXPECTED_MESSAGE);
            assertTrue(logCaptor.getErrorLogs().get(0).contains("Failed to validate the schema with error message"),
                       "Error log did not contain message");
        }
    }
}
