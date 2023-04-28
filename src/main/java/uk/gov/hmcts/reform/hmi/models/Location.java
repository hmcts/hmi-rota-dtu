package uk.gov.hmcts.reform.hmi.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Class that represents a location.
 */
@Entity
@Data
public class Location {

    /**
     * The id of the location.
     */
    @Id
    @JsonProperty("locationId")
    private Integer id;

    /**
     * The name of the location.
     */
    private String name;
}
