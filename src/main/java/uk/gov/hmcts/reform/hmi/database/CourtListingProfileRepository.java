package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.CourtListingProfile;

import java.util.Optional;

@Repository
public interface CourtListingProfileRepository extends JpaRepository<CourtListingProfile, String> {
    @Override
    Optional<CourtListingProfile> findById(String id);
}

