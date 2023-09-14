package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.hmi.database.CourtListingProfileRepository;
import uk.gov.hmcts.reform.hmi.database.JusticeRepository;
import uk.gov.hmcts.reform.hmi.database.LocationRepository;
import uk.gov.hmcts.reform.hmi.database.ScheduleRepository;
import uk.gov.hmcts.reform.hmi.database.VenueRepository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;
import uk.gov.hmcts.reform.hmi.models.Justice;
import uk.gov.hmcts.reform.hmi.models.Location;
import uk.gov.hmcts.reform.hmi.models.Schedule;
import uk.gov.hmcts.reform.hmi.models.Venue;

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
    private VenueRepository venueRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private CourtListingProfileRepository courtListingProfileRepository;

    private static final String ROTA_VALID_XML = "mocks/rotaValidFile.xml";

    private static final String EXPECTED_MESSAGE = "Expected and actual don't match";

    private static final String SCHEDULE_ID = "CS123_CHAIR";
    private static final String COURT_LISTING_PROFILE_ID = "CS123";
    private static final String JUDGE_ID = "JOH1";
    LogCaptor logCaptor = LogCaptor.forClass(ConversionService.class);
    List<String> uniqueClpIds = new ArrayList<>();
    Schedule schedule = new Schedule();
    Justice justice = new Justice();
    CourtListingProfile courtListingProfile = new CourtListingProfile();

    @BeforeEach
    void setup() {
        uniqueClpIds.clear();
        uniqueClpIds.add(COURT_LISTING_PROFILE_ID);


        schedule = new Schedule();
        schedule.setId(SCHEDULE_ID);
        schedule.setSlot("CHAIR");
        schedule.setJusticeId(JUDGE_ID);
        schedule.setCourtListingProfileId(COURT_LISTING_PROFILE_ID);

        justice = new Justice();
        justice.setId(JUDGE_ID);
        justice.setEmail("test@test.com");

        courtListingProfile = new CourtListingProfile();
        courtListingProfile.setId(COURT_LISTING_PROFILE_ID);
        courtListingProfile.setSession("AM");
        courtListingProfile.setSessionDate(LocalDate.now());
        courtListingProfile.setPanel("ADULT");
        courtListingProfile.setBusiness("APP");
        courtListingProfile.setLocationId("1");
        courtListingProfile.setVenueId("1");
    }

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
    void testCreateRequestJson() {
        when(scheduleRepository.getUniqueClpIds()).thenReturn(uniqueClpIds);
        when(scheduleRepository.findByCourtListingProfileId(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(List.of(schedule)));
        when(justiceRepository.findById(JUDGE_ID)).thenReturn(Optional.of(justice));
        when(courtListingProfileRepository.findById(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(courtListingProfile));

        Venue venue = new Venue();
        venue.setId(1);
        venue.setName("Test venue name");
        when(venueRepository.findById(1)).thenReturn(Optional.of(venue));

        Location location = new Location();
        location.setId(1);
        location.setName("Test location name");
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));

        Map<String, String> requestsJson = conversionService.createRequestJson();
        requestsJson.forEach((k,v) -> {
            try {
                JsonNode node = new ObjectMapper().readTree(v);
                if (!node.isEmpty()) {
                    assertEquals("Test location name Test venue name",
                                 node.get("session").get("room").get("locationId").asText(),
                                 EXPECTED_MESSAGE);
                }
            } catch (JsonProcessingException e) {
                return;
            }
        });
        assertFalse(requestsJson.isEmpty(), EXPECTED_MESSAGE);
    }

    @Test
    void testCreateRequestJsonEmptyVenue() {
        when(scheduleRepository.getUniqueClpIds()).thenReturn(uniqueClpIds);
        when(scheduleRepository.findByCourtListingProfileId(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(List.of(schedule)));
        when(justiceRepository.findById(JUDGE_ID)).thenReturn(Optional.of(justice));
        when(courtListingProfileRepository.findById(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(courtListingProfile));

        when(venueRepository.findById(1)).thenReturn(Optional.empty());

        Location location = new Location();
        location.setId(1);
        location.setName("Test location name");
        when(locationRepository.findById(1)).thenReturn(Optional.of(location));

        Map<String, String> requestsJson = conversionService.createRequestJson();
        assertFalse(requestsJson.isEmpty(), EXPECTED_MESSAGE);

        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Error getting venue name from venue id within court listing profile."),
                   "Error log did not contain message");
    }

    @Test
    void testCreateRequestJsonEmptyLocation() {
        when(scheduleRepository.getUniqueClpIds()).thenReturn(uniqueClpIds);
        when(scheduleRepository.findByCourtListingProfileId(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(List.of(schedule)));
        when(justiceRepository.findById(JUDGE_ID)).thenReturn(Optional.of(justice));
        when(courtListingProfileRepository.findById(COURT_LISTING_PROFILE_ID))
            .thenReturn(Optional.of(courtListingProfile));

        Venue venue = new Venue();
        venue.setId(1);
        venue.setName("Test venue name");
        when(venueRepository.findById(1)).thenReturn(Optional.of(venue));

        when(locationRepository.findById(1)).thenReturn(Optional.empty());

        Map<String, String> requestsJson = conversionService.createRequestJson();
        assertFalse(requestsJson.isEmpty(), EXPECTED_MESSAGE);

        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Error getting location name from location id within court listing profile."),
                   "Error log did not contain message");
    }
}
