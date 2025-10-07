package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.AiidaRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

import java.util.Optional;
import java.util.UUID;

public interface AiidaRecordRepository extends JpaRepository<AiidaRecord, Long> {
    Optional<AiidaRecord> findFirstByDataSourceIdOrderByIdDesc(UUID dataSourceId);
    long deleteAiidaRecordsByTimestampBefore(Instant threshold);
}
