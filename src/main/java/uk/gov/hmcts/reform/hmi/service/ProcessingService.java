package uk.gov.hmcts.reform.hmi.service;

import com.azure.core.exception.AzureException;
import com.azure.storage.blob.models.BlobItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;
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

    private final JusticeRepository justiceRepository;

    private final LocationRepository locationRepository;

    private final VenueRepository venueRepository;

    private final CourtListingProfileRepository courtListingProfileRepository;

    private final ScheduleRepository scheduleRepository;

    ObjectMapper mapper = new ObjectMapper();

    public ProcessingService(ValidationService validationService, AzureBlobService azureBlobService,
                             ConversionService conversionService, JusticeRepository justiceRepository,
                             LocationRepository locationRepository, VenueRepository venueRepository,
                             CourtListingProfileRepository courtListingProfileRepository,
                             ScheduleRepository scheduleRepository) {
        this.validationService = validationService;
        this.azureBlobService = azureBlobService;
        this.conversionService = conversionService;
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
        boolean isFileValid = validationService.isValid(blobData);

        log.info(String.format("Blob %s validation: %s", blob.getName(), isFileValid));

        if (isFileValid) {
            //CONVERT XML TO JSON
            JsonNode json = conversionService.convertXmlToJson(blobData);
            processJsonInToDatabase(json);
        } else {
            //RAISE SERVICE NOW REQUEST
            return false;
        }

        return true;
    }

    private void processJsonInToDatabase(JsonNode json) {
        List<Justice> justices = new ArrayList<>();
        List<Location> locations = new ArrayList<>();
        List<Venue> venues = new ArrayList<>();
        List<CourtListingProfile> courtListingProfiles = new ArrayList<>();
        List<Schedule> schedules = new ArrayList<>();

        // Convert jsonNodes into models and add to lists
        json.get("magistrates").get("magistrate").forEach(magistrate ->
            justices.add(mapper.convertValue(magistrate, Justice.class)));
        json.get("districtJudges").get("districtJudge").forEach(districtJudge ->
            justices.add(mapper.convertValue(districtJudge, Justice.class)));

        json.get("locations").get("location").forEach(location ->
            locations.add(mapper.convertValue(location, Location.class)));

        json.get("venues").get("venue").forEach(venue ->
            venues.add(mapper.convertValue(venue, Venue.class)));

        json.get("courtListingProfiles").get("courtListingProfile").forEach(courtListingProfile ->
            courtListingProfiles.add(mapper.convertValue(courtListingProfile, CourtListingProfile.class)));

        json.get("schedules").get("schedule").forEach(schedule -> {
            schedules.add(new Schedule(
                schedule.get("id").asText(),
                schedule.get("courtListingProfile").get("idref").asText(),
                schedule.get("justice").get("idref").asText(),
                schedule.get("slot").asText()
            ));
        });


        // Save all data
        justiceRepository.saveAll(justices);
        locationRepository.saveAll(locations);
        venueRepository.saveAll(venues);
        courtListingProfileRepository.saveAll(courtListingProfiles);
        scheduleRepository.saveAll(schedules);
    }

    private void moveFileToProcessingContainer(BlobItem blob) {
        try {
            // Lease it for 60 seconds
            String leaseId = azureBlobService.acquireBlobLease(blob.getName());

            // Break the lease and copy blob for processing
            azureBlobService.copyBlobToProcessingContainer(blob.getName(), leaseId);
        } catch (AzureException ex) {
            log.error(String.format("Failed to move the blob %s to processing container with error message: %s",
                                    blob.getName(), ex.getMessage()));
        }
    }
}
