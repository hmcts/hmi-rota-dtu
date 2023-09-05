package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
import uk.gov.hmcts.reform.hmi.config.ValidationConfiguration;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@SuppressWarnings("PMD")
public class ProcessingService {

    private final ValidationService validationService;

    private final AzureBlobService azureBlobService;

    private final ConversionService conversionService;

    private final ServiceNowService serviceNowService;

    private final ValidationConfiguration validationConfiguration;

    private final JusticeRepository justiceRepository;

    private final LocationRepository locationRepository;

    private final VenueRepository venueRepository;

    private final CourtListingProfileRepository courtListingProfileRepository;

    private final ScheduleRepository scheduleRepository;

    private static final String EXCEPTION_MESSAGE = "Issue processing json into the database";

    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                                                       true);

    @Autowired
    public ProcessingService(ValidationService validationService,
                             AzureBlobService azureBlobService,
                             ConversionService conversionService,
                             ServiceNowService serviceNowService,
                             ValidationConfiguration validationConfiguration,
                             JusticeRepository justiceRepository,
                             LocationRepository locationRepository,
                             VenueRepository venueRepository,
                             CourtListingProfileRepository courtListingProfileRepository,
                             ScheduleRepository scheduleRepository) {
        this.validationService = validationService;
        this.azureBlobService = azureBlobService;
        this.conversionService = conversionService;
        this.serviceNowService = serviceNowService;
        this.validationConfiguration = validationConfiguration;
        this.justiceRepository = justiceRepository;
        this.locationRepository = locationRepository;
        this.venueRepository = venueRepository;
        this.courtListingProfileRepository = courtListingProfileRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public Map<String, String> processFile(BlobItem blob) throws IOException, SAXException {

        //MOVE FILE TO PROCESSING CONTAINER
        moveFileToProcessingContainer(blob);

        //READ FILE FROM CONTAINER
        byte[] blobData  = azureBlobService.downloadBlob(blob.getName());
        log.info(String.format("Download blob %s", blob.getName()));

        //VALIDATE XML FILE AGAINST SCHEMA FILE PROVIDED BY ROTA
        boolean isFileValid = validationService.isValid(validationConfiguration.getRotaHmiXsd(), blobData);

        log.info(String.format("Blob %s validation: %s", blob.getName(), isFileValid));

        if (isFileValid) {
            JsonNode rotaJson = conversionService.convertXmlToJson(blobData);
            StringBuilder result = saveRotaJsonIntoDatabase(rotaJson);
            if (!result.isEmpty()) {
                serviceNowService.createServiceNowRequest(result,
                    String.format("Unable to save file %s in database", blob.getName()));
                return new HashMap<>();
            }
            return conversionService.createRequestJson();
        } else {
            StringBuilder error = new StringBuilder();
            error.append(String.format("Unable to validate file %s against rota XML schema", blob.getName()));
            serviceNowService.createServiceNowRequest(error, "XML file validation failed");
            return Collections.emptyMap();
        }
    }

    private StringBuilder saveRotaJsonIntoDatabase(JsonNode json) {
        StringBuilder errors = new StringBuilder();
        String newLine = "\n";
        if (!handleJusticesToModel(json.get("magistrates").get("magistrate"))) {
            errors.append("Unable to save magistrates in database").append(newLine);
        }

        if (!handleJusticesToModel(json.get("districtJudges").get("districtJudge"))) {
            errors.append("Unable to save districtJudges in database").append(newLine);
        }

        if (!handleLocationsToModel(json.get("locations").get("location"))) {
            errors.append("Unable to save locations in database").append(newLine);
        }

        if (!handleVenuesToModel(json.get("venues").get("venue"))) {
            errors.append("Unable to save venues in database").append(newLine);
        }

        if (!handleCourtListingProfilesToModel(json.get("courtListingProfiles").get("courtListingProfile"))) {
            errors.append("Unable to save courtListingProfiles in database").append(newLine);
        }

        if (!handleSchedulesToModel(json.get("schedules").get("schedule"))) {
            errors.append("Unable to save schedules in database").append(newLine);
        }

        return errors;
    }

    /**
     * Take in the jsonNode for either magistrates or district judges, convert to a model and store in the database.
     * @param justiceJsonNode The justice jsonNode.
     */
    private boolean handleJusticesToModel(JsonNode justiceJsonNode) {
        List<Justice> justices = new ArrayList<>();
        AtomicBoolean saved = new AtomicBoolean(true);
        if (justiceJsonNode != null) {
            justiceJsonNode.forEach(justice -> {
                try {
                    justices.add(mapper.treeToValue(justice, Justice.class));
                } catch (JsonProcessingException ex) {
                    log.error(EXCEPTION_MESSAGE, ex.getMessage());
                    saved.set(false);
                }
            });
            justiceRepository.saveAll(justices);
        }
        return saved.get();
    }

    /**
     * Take in the location json node, convert to a model and store in the database.
     * @param locations The locations jsonNode.
     */
    private boolean handleLocationsToModel(JsonNode locations) {
        List<Location> locationsList = new ArrayList<>();
        AtomicBoolean saved = new AtomicBoolean(true);
        if (locations != null) {
            locations.forEach(location -> {
                try {
                    locationsList.add(mapper.treeToValue(location, Location.class));
                } catch (JsonProcessingException ex) {
                    log.error(EXCEPTION_MESSAGE, ex.getMessage());
                    saved.set(false);
                }
            });
            locationRepository.saveAll(locationsList);
        }
        return saved.get();
    }

    /**
     * Take in the venue json node, convert to a model and store in the database.
     * @param venues The venues jsonNode.
     */
    private boolean handleVenuesToModel(JsonNode venues) {
        List<Venue> venuesList = new ArrayList<>();
        AtomicBoolean saved = new AtomicBoolean(true);
        if (venues != null) {
            venues.forEach(venue -> {
                try {
                    venuesList.add(mapper.treeToValue(venue, Venue.class));
                } catch (JsonProcessingException ex) {
                    log.error(EXCEPTION_MESSAGE, ex.getMessage());
                    saved.set(false);
                }
            });
            venueRepository.saveAll(venuesList);
        }
        return saved.get();
    }

    /**
     * Take in the court listing profile json node, convert to a model and store in the database.
     * @param courtListingProfiles The court listing profile jsonNode.
     */
    private boolean handleCourtListingProfilesToModel(JsonNode courtListingProfiles) {
        List<CourtListingProfile> courtListingProfileList = new ArrayList<>();
        AtomicBoolean saved = new AtomicBoolean(true);
        if (courtListingProfiles != null) {
            courtListingProfiles.forEach(courtListingProfile -> {
                try {
                    JsonNode businessNode = courtListingProfile.get("business");
                    if (businessNode != null && !"CRC".equals(businessNode.asText())) {
                        courtListingProfileList.add(mapper.treeToValue(
                            courtListingProfile,
                            CourtListingProfile.class
                        ));
                    }
                } catch (JsonProcessingException ex) {
                    log.error(EXCEPTION_MESSAGE, ex.getMessage());
                    saved.set(false);
                }
            });
            courtListingProfileRepository.saveAll(courtListingProfileList);
        }
        return saved.get();
    }

    /**
     * Take in the schedules json node, convert to a model and store in the database.
     * @param schedules The schedules jsonNode.
     */
    private boolean handleSchedulesToModel(JsonNode schedules) {
        List<Schedule> scheduleList = new ArrayList<>();
        AtomicBoolean saved = new AtomicBoolean(true);
        try {
            if (schedules != null) {
                schedules.forEach(schedule -> scheduleList.add(new Schedule(
                    schedule.get("id").textValue(),
                    schedule.get("courtListingProfile").get("idref").textValue(),
                    schedule.get("justice").get("idref").textValue(),
                    schedule.get("slot").textValue()
                )));
                scheduleRepository.saveAll(scheduleList);
            }
        } catch (Exception ex) {
            log.error(EXCEPTION_MESSAGE, ex.getMessage());
            saved.set(false);
        }
        return saved.get();
    }

    private void moveFileToProcessingContainer(BlobItem blob) {
        // Lease it for 60 seconds
        String leaseId = azureBlobService.acquireBlobLease(blob.getName());

        // Break the lease and copy blob for processing
        azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);

        // Delete the original blob
        azureBlobService.deleteOriginalBlob(blob.getName());
    }
}
