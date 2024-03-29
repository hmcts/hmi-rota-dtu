package uk.gov.hmcts.reform.hmi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.hmi.database.CourtListingProfileRepository;
import uk.gov.hmcts.reform.hmi.database.JusticeRepository;
import uk.gov.hmcts.reform.hmi.database.LocationRepository;
import uk.gov.hmcts.reform.hmi.database.ScheduleRepository;
import uk.gov.hmcts.reform.hmi.database.VenueRepository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;
import uk.gov.hmcts.reform.hmi.models.Justice;
import uk.gov.hmcts.reform.hmi.models.Schedule;
import uk.gov.hmcts.reform.hmi.models.Venue;
import uk.gov.hmcts.reform.hmi.models.external.HmiJsonRequest;
import uk.gov.hmcts.reform.hmi.models.external.Judge;
import uk.gov.hmcts.reform.hmi.models.external.Location;
import uk.gov.hmcts.reform.hmi.models.external.LocationType;
import uk.gov.hmcts.reform.hmi.models.external.Session;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConversionService {

    private final ScheduleRepository scheduleRepository;

    private final JusticeRepository justiceRepository;

    private final CourtListingProfileRepository courtListingProfileRepository;

    private final LocationRepository locationRepository;

    private final VenueRepository venueRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String EXCEPTION_MESSAGE = "Issue converting model to json";

    public ConversionService(ScheduleRepository scheduleRepository, JusticeRepository justiceRepository,
                             CourtListingProfileRepository courtListingProfileRepository,
                             LocationRepository locationRepository, VenueRepository venueRepository) {
        this.scheduleRepository = scheduleRepository;
        this.justiceRepository = justiceRepository;
        this.courtListingProfileRepository = courtListingProfileRepository;
        this.locationRepository = locationRepository;
        this.venueRepository = venueRepository;
    }

    public JsonNode convertXmlToJson(byte[] rotaXml) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            return xmlMapper.readTree(rotaXml);
        } catch (IOException e) {
            log.error(String.format("Failed to convert the blob to Json with error message: %s", e.getMessage()));
        }

        return null;
    }

    public Map<String, String> createRequestJson() {
        Map<String, String> requestMap = new ConcurrentHashMap<>();
        List<String> uniqueClpIds = scheduleRepository.getUniqueClpIds();
        uniqueClpIds.forEach(clpId -> {
            HmiJsonRequest hmiJsonRequest = new HmiJsonRequest(formatRequestJson(clpId));
            try {
                String json = MAPPER.writeValueAsString(hmiJsonRequest);
                requestMap.put(clpId, json);
            } catch (JsonProcessingException ex) {
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });

        return requestMap;
    }

    public Session formatRequestJson(String clpId) {
        Session session = new Session();
        Optional<List<Schedule>> schedules = scheduleRepository.findByCourtListingProfileId(clpId);
        if (schedules.isPresent()) {
            List<Judge> judges = formatJudges(schedules.get());
            session.setJohs(judges);
        }

        Optional<CourtListingProfile> courtListingProfile = courtListingProfileRepository.findById(clpId);
        if (courtListingProfile.isPresent()) {
            session.setRoom(formatLocation(courtListingProfile.get(), true));
            session.setSessionReference(courtListingProfile.get().getId());
            session.setSessionStart(calculateSessionStartTime(courtListingProfile.get()));
            session.setVenue(formatLocation(courtListingProfile.get(), false));
        }

        return session;
    }

    private List<Judge> formatJudges(List<Schedule> schedules) {
        List<Judge> judges = new ArrayList<>();
        schedules.forEach(schedule -> {
            Optional<Justice> justice = justiceRepository.findById(schedule.getJusticeId());
            justice.ifPresent(value -> judges.add(new Judge(value.getEmail(), schedule.getSlot())));
        });

        return judges;
    }

    private Location formatLocation(CourtListingProfile courtListingProfile, boolean isRoom) {
        Optional<uk.gov.hmcts.reform.hmi.models.Location> optionalLocation =
            locationRepository.findById(Integer.parseInt(courtListingProfile.getLocationId()));
        String locationName = "";
        if (optionalLocation.isPresent()) {
            locationName = optionalLocation.get().getName();
        }

        if (isRoom) {
            Optional<Venue> optionalVenue =
                venueRepository.findById(Integer.parseInt(courtListingProfile.getVenueId()));
            String roomName = "";
            if (optionalVenue.isPresent()) {
                roomName = String.format("%s %s", locationName,
                                         optionalVenue.get().getName());
            }

            if (roomName.isEmpty()) {
                log.error("Error getting venue name from venue id within court listing profile.");
            } else {
                return new Location(roomName, LocationType.ROOM.label);
            }
        }

        if (locationName.isEmpty()) {
            log.error("Error getting location name from location id within court listing profile.");
        } else {
            return new Location(locationName, LocationType.COURT.label);
        }

        return null;
    }

    private String calculateSessionStartTime(CourtListingProfile courtListingProfile) {
        if ("AM".equals(courtListingProfile.getSession())) {
            return courtListingProfile.getSessionDate().toString() + "T10:00:00Z";
        }

        return courtListingProfile.getSessionDate().toString() + "T14:00:00Z";
    }
}
