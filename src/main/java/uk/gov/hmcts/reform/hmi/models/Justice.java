package uk.gov.hmcts.reform.hmi.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

/**
 * Class that represents a justice (either a Magistrate or District Judge).
 */
@Entity
@Data
public class Justice {

    /**
     * The id of the Magistrate or District Judge.
     */
    @Id
    private String id;

    /**
     * The title of the Magistrate or District Judge.
     */
    private String title;

    /**
     * The forenames of the Magistrate or District Judge.
     */
    private String forenames;

    /**
     * The surname of the Magistrate or District Judge.
     */
    private String surname;

    /**
     * The email of the Magistrate or District Judge.
     */
    private String email;
}
