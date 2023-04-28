package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.Schedule;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {
    Optional<List<Schedule>> findByCourtListingProfileId(String courtListingProfileId);
    @Query(value = "select distinct COURT_LISTING_PROFILE_ID "
        + "from SCHEDULE",
          nativeQuery = true)
    List<String> getUniqueClpIds();
}

