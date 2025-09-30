package energy.eddie.aiida.repositories;

import energy.eddie.aiida.models.record.InboundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InboundRecordRepository extends JpaRepository<InboundRecord, Long> {
    Optional<InboundRecord> findTopByDataSourceIdOrderByTimestampDesc(UUID dataSourceId);

    long deleteInboundRecordsByTimestampBefore(Instant threshold);
}
