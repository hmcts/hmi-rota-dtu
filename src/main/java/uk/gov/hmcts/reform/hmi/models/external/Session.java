package uk.gov.hmcts.reform.hmi.models.external;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Data
@Getter
@Setter
public class Session {
    private List<Judge> johs = new ArrayList<>();
    private Location room;
    /**
     * If Session in CLP table is AM, duration will be 180, otherwise 120.
     */
    private Integer sessionDuration;
    /**
     * ID from the CLP table
     */
    private String sessionReference;
    /**
     * If Session in CLP table is AM, sessionStart time will be 10:00, otherwise 14:00.
     */
    private String sessionStart;
    private Location venue;
}
