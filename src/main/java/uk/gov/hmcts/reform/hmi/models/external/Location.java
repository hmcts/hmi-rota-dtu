package uk.gov.hmcts.reform.hmi.models.external;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Location {
    /**
     * If LocationType is Room, locationId will come from CLP table (venueId)
     * If LocationType is Court, locationId will come from CLP table (locationId)
     */
    private String locationId;
    /**
     * This will be ROTA
     */
    private String locationReferenceId;

    /**
     * LocationType can be Room or Court
     */
    private String locationType;

    public Location(String locationId, String locationType) {
        this.locationId = locationId;
        this.setLocationReferenceId("ROTA");
        this.locationType = locationType;
    }
}
