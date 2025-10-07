package energy.eddie.aiida.models.record;

import java.time.Instant;

public record LatestRecordSchema(Instant sentAt, String message) {
}
