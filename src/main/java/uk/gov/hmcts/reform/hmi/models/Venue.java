package uk.gov.hmcts.reform.hmi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("venueId")
    private Integer id;

    /**
     * The name of the venue.
     */
    private String name;
}
