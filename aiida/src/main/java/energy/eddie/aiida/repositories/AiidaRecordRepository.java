package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiidaRecordRepository extends JpaRepository<AiidaRecord, Long> {
}
