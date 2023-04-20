package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;

@Repository
public interface CourtListingProfileRepository extends JpaRepository<CourtListingProfile, String> {
}

