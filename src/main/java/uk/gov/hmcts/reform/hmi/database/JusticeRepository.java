package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.Justice;

@Repository
public interface JusticeRepository extends JpaRepository<Justice, String> {
}
