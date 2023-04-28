package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
}
