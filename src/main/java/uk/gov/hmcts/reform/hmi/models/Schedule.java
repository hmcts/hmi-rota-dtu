package uk.gov.hmcts.reform.hmi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class that represents a schedule.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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
