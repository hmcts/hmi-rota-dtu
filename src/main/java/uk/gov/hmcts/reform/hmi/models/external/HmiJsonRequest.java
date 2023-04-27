package uk.gov.hmcts.reform.hmi.models.external;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class HmiJsonRequest {
    /**
     * The session will contain all information about judges allocation.
     */
    private Session session;
}
