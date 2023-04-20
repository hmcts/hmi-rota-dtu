package uk.gov.hmcts.reform.hmi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Class that represents a justice (either a Magistrate or District Judge).
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
    private LocalDateTime lastUpdated;
}
