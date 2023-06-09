package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.JsonNode;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.hmi.database.CourtListingProfileRepository;
import uk.gov.hmcts.reform.hmi.database.JusticeRepository;
import uk.gov.hmcts.reform.hmi.database.ScheduleRepository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;
import uk.gov.hmcts.reform.hmi.models.Justice;
import uk.gov.hmcts.reform.hmi.models.Schedule;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
@ActiveProfiles("test")
class ConversionServiceTest {

    @InjectMocks
    private ConversionService conversionService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private JusticeRepository justiceRepository;

    @Mock
    private CourtListingProfileRepository courtListingProfileRepository;

    private static final String ROTA_VALID_XML = "mocks/rotaValidFile.xml";

    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    private static final String SCHEDULE_ID = "CS123_CHAIR";
    private static final String COURT_LISTING_PROFILE_ID = "CS123";
    private static final String JUDGE_ID = "JOH1";
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

    @Test
    void tesCreateRequestJson() {
        List<String> uniqueClpIds = new ArrayList<>();
        uniqueClpIds.add(COURT_LISTING_PROFILE_ID);
        when(scheduleRepository.getUniqueClpIds()).thenReturn(uniqueClpIds);

        Schedule schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setSlot("CHAIR");
        schedule.setJusticeId(JUDGE_ID);
        schedule.setCourtListingProfileId(COURT_LISTING_PROFILE_ID);
        when(scheduleRepository.findByCourtListingProfileId(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(List.of(schedule)));

        Justice justice = new Justice();
        justice.setId(JUDGE_ID);
        justice.setEmail("test@test.com");
        when(justiceRepository.findById(JUDGE_ID)).thenReturn(Optional.of(justice));

        CourtListingProfile courtListingProfile = new CourtListingProfile();
        courtListingProfile.setId(COURT_LISTING_PROFILE_ID);
        courtListingProfile.setSession("AM");
        courtListingProfile.setSessionDate(LocalDate.now());
        courtListingProfile.setPanel("ADULT");
        courtListingProfile.setBusiness("APP");
        courtListingProfile.setLocationId("LOC1");
        courtListingProfile.setVenueId("VEN1");

        when(courtListingProfileRepository.findById(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(courtListingProfile));

        Map<String, String> requestsJson = conversionService.createRequestJson();
        assertFalse(requestsJson.isEmpty(), EXPECTED_MESSAGE);
    }
}
