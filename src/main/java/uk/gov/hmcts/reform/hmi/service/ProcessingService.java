package uk.gov.hmcts.reform.hmi.service;

import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;

@Slf4j
@Service
public class ProcessingService {

    private final ValidationService validationService;

    private final AzureBlobService azureBlobService;

    private final ConversionService conversionService;

    private final ValidationConfiguration validationConfiguration;

    private final JusticeRepository justiceRepository;

    private final LocationRepository locationRepository;

    private final VenueRepository venueRepository;

    private final CourtListingProfileRepository courtListingProfileRepository;

    private final ScheduleRepository scheduleRepository;

    private static final String EXCEPTION_MESSAGE = "Issue processing json into the database";

    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                                                       true);


    public ProcessingService(ValidationService validationService, AzureBlobService azureBlobService,
                             ConversionService conversionService, ValidationConfiguration validationConfiguration,
                             JusticeRepository justiceRepository,
                             LocationRepository locationRepository, VenueRepository venueRepository,
                             CourtListingProfileRepository courtListingProfileRepository,
                             ScheduleRepository scheduleRepository) {
        this.validationService = validationService;
        this.azureBlobService = azureBlobService;
        this.conversionService = conversionService;
        this.validationConfiguration = validationConfiguration;
        this.justiceRepository = justiceRepository;
        this.locationRepository = locationRepository;
        this.venueRepository = venueRepository;
        this.courtListingProfileRepository = courtListingProfileRepository;
        this.scheduleRepository = scheduleRepository;
    }

    public boolean processFile(BlobItem blob) throws IOException, SAXException {

        //MOVE FILE TO PROCESSING CONTAINER
        moveFileToProcessingContainer(blob);

        //READ FILE FROM CONTAINER
        byte[] blobData  = azureBlobService.downloadBlob(blob.getName());
        log.info(String.format("Download blob %s", blob.getName()));

        //VALIDATE XML FILE AGAINST SCHEMA FILE PROVIDED BY ROTA
        boolean isFileValid = validationService.isValid(validationConfiguration.getRotaHmiXsd(), blobData);

        log.info(String.format("Blob %s validation: %s", blob.getName(), isFileValid));

        if (isFileValid) {
            //CONVERT XML TO JSON
            JsonNode rotaJson = conversionService.convertXmlToJson(blobData);
            saveRotaJsonIntoDatabase(rotaJson);
            conversionService.createRequestJson();
        } else {
            //RAISE SERVICE NOW REQUEST
            return false;
        }

        return true;
    }

    private void saveRotaJsonIntoDatabase(JsonNode json) {
        handleJusticesToModel(json.get("magistrates").get("magistrate"),
                              json.get("districtJudges").get("districtJudge"));
        handleLocationsToModel(json.get("locations").get("location"));
        handleVenuesToModel(json.get("venues").get("venue"));
        handleCourtListingProfilesToModel(json.get("courtListingProfiles").get("courtListingProfile"));
        handleSchedulesToModel(json.get("schedules").get("schedule"));
    }

    /**
     * Take in the magistrates and districtJudge json nodes, convert to a model and store in the database.
     * @param magistrates The magistrates jsonNode.
     * @param districtJudges The district judge jsonNode.
     */
    private void handleJusticesToModel(JsonNode magistrates, JsonNode districtJudges) {
        List<Justice> justices = new ArrayList<>();
        magistrates.forEach(magistrate -> {
            try {
                justices.add(mapper.treeToValue(magistrate, Justice.class));
            } catch (JsonProcessingException ex) {
                // TODO Raise incident in snow
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });

        districtJudges.forEach(districtJudge -> {
            try {
                justices.add(mapper.treeToValue(districtJudge, Justice.class));
            } catch (JsonProcessingException ex) {
                // TODO Raise incident in snow
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });
        justiceRepository.saveAll(justices);
    }

    /**
     * Take in the location json node, convert to a model and store in the database.
     * @param locations The locations jsonNode.
     */
    private void handleLocationsToModel(JsonNode locations) {
        List<Location> locationsList = new ArrayList<>();
        locations.forEach(location -> {
            try {
                locationsList.add(mapper.treeToValue(location, Location.class));
            } catch (JsonProcessingException ex) {
                // TODO Raise incident in snow
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });
        locationRepository.saveAll(locationsList);
    }

    /**
     * Take in the venue json node, convert to a model and store in the database.
     * @param venues The venues jsonNode.
     */
    private void handleVenuesToModel(JsonNode venues) {
        List<Venue> venuesList = new ArrayList<>();
        venues.forEach(venue -> {
            try {
                venuesList.add(mapper.treeToValue(venue, Venue.class));
            } catch (JsonProcessingException ex) {
                // TODO Raise incident in snow
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });
        venueRepository.saveAll(venuesList);
    }

    /**
     * Take in the court listing profile json node, convert to a model and store in the database.
     * @param courtListingProfiles The court listing profile jsonNode.
     */
    private void handleCourtListingProfilesToModel(JsonNode courtListingProfiles) {
        List<CourtListingProfile> courtListingProfileList = new ArrayList<>();
        courtListingProfiles.forEach(courtListingProfile -> {
            try {
                courtListingProfileList.add(mapper.treeToValue(courtListingProfile,
                    CourtListingProfile.class));
            } catch (JsonProcessingException ex) {
                // TODO Raise incident in snow
                log.error(EXCEPTION_MESSAGE, ex.getMessage());
            }
        });
        courtListingProfileRepository.saveAll(courtListingProfileList);
    }

    /**
     * Take in the schedules json node, convert to a model and store in the database.
     * @param schedules The schedules jsonNode.
     */
    private void handleSchedulesToModel(JsonNode schedules) {
        List<Schedule> scheduleList = new ArrayList<>();
        schedules.forEach(schedule -> {
            scheduleList.add(new Schedule(
                schedule.get("id").textValue(),
                schedule.get("courtListingProfile").get("idref").textValue(),
                schedule.get("justice").get("idref").textValue(),
                schedule.get("slot").textValue()
            ));
        });
        scheduleRepository.saveAll(scheduleList);
    }

    private void moveFileToProcessingContainer(BlobItem blob) {
        // Lease it for 60 seconds
        String leaseId = azureBlobService.acquireBlobLease(blob.getName());

        // Break the lease and copy blob for processing
        azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);
    }
}
