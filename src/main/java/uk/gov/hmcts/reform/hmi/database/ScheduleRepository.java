package uk.gov.hmcts.reform.hmi.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.hmi.models.Schedule;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, String> {
}

