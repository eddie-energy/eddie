package energy.eddie.api.agnostic.retransmission.result;

import java.time.ZonedDateTime;

public record Failure(
        String permissionId,
        ZonedDateTime timestamp,
        String reason
) implements RetransmissionResult {
}
