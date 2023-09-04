package uk.gov.hmcts.reform.hmi.models.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class Judge {

    /**
     * The id will be the email address of the Magistrate or District Judge.
     */
    private String johId;

    /**
     * The slot will come from session.
     */
    private String slot;

    /**
     * If Slot is chair, isPresiding will be TRUE, else FALSE.
     */
    @JsonProperty("isPresiding")
    private boolean isPresiding;

    public Judge(String johId, String slot) {
        this.johId = johId;
        this.slot = slot;
        this.isPresiding = switch (slot) {
            case "CHAIR", "SINGLE_JUSTICE", "DISTRICT_JUDGE" -> true;
            default -> false;
        };
    }
}
