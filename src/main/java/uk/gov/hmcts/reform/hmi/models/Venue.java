package uk.gov.hmcts.reform.hmi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Class that represents a venue.
 */
@Entity
@Data
public class Venue {

    /**
     * The id of the venue.
     */
    @Id
    private Integer id;

    /**
     * The name of the venue.
     */
    private String name;
}
