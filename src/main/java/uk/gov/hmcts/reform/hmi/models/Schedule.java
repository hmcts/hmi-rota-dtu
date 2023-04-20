package uk.gov.hmcts.reform.hmi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Class that represents a schedule.
 */
@Entity
@Data
public class Schedule {

    /**
     * The id of the schedule.
     */
    @Id
    private String id;

    /**
     * The id of the court listing profile record.
     */
    private String courtListingProfileId;

    /**
     * The id of the justice.
     */
    private String justiceId;

    /**
     * The slot of the justice.
     */
    private String slot;
}
