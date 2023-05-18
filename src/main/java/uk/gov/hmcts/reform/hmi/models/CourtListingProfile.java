package uk.gov.hmcts.reform.hmi.models;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Class that represents a court listing profile.
 */
@Entity
@Data
public class CourtListingProfile {

    /**
     * The id of the court listing profile.
     */
    @Id
    private String id;

    /**
     * The date of the session.
     */
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate sessionDate;

    /**
     * The time of session (AM/PM).
     */
    private String session;

    /**
     * The panel of the court listing profile.
     */
    private String panel;

    /**
     * The business of the court listing profile.
     */
    private String business;

    /**
     * The location id of the court listing profile.
     */
    private String locationId;

    /**
     * If it's a Welsh speaking court listing profile.
     */
    private boolean welshSpeaking;

    /**
     * The last updated date of the court listing profile.
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedDate;

    /**
     * The id of the linked session.
     */
    private String linkedSessionId;

    /**
     * The id of the venue.
     */
    private String venueId;

    /**
     * The requestJson for HMI.
     */
    private String requestJson;

    /**
     * The errorMessage if HMI request fails.
     */
    private String errorMessage;
}
