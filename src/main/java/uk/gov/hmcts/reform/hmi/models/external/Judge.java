package uk.gov.hmcts.reform.hmi.models.external;

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
     * The slot will come from session
     */
    private String slot;
    /**
     * If Slot is chair, isPresiding will be TRUE, else FALSE
     */
    private boolean isPresiding;

    public Judge(String johId, String slot) {
        this.johId = johId;
        this.slot = slot;
        this.isPresiding = slot.equals("CHAIR");
    }
}
