package uk.gov.hmcts.reform.hmi.models.external;

public enum LocationType {
    ROOM("Room"),
    COURT("Court");

    public final String label;

    LocationType(String label) {
        this.label = label;
    }
}
