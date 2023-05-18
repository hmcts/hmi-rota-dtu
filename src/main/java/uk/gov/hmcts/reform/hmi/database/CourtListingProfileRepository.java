package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;

import java.util.Optional;

@Repository
public interface CourtListingProfileRepository extends JpaRepository<CourtListingProfile, String> {

    String COURT_LISTING_PROFILE_ID = "id";
    String ERROR_MESSAGE = "errormessage";
    String REQUEST_JSON = "requestjson";
    @Override
    Optional<CourtListingProfile> findById(String id);

    @Modifying
    @Query(value = "UPDATE courtlistingprofile " +
        "SET errormessage = :errormessage, " +
        "requestjson = :requestjson " +
        "WHERE id = :id", nativeQuery = true)
    void updateCourtListingProfileWithError(@Param(COURT_LISTING_PROFILE_ID) String courtListingProfileId,
                                            @Param(ERROR_MESSAGE) String errorMessage,
                                            @Param(REQUEST_JSON) String requestJson);
}

